package py.edu.ucom.is2.proyectocamel.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.MensajeTransferencia;
import py.edu.ucom.is2.proyectocamel.model.ResultadoTransferencia;

@Component
@ConditionalOnProperty(name = "sipap.jms.habilitado", havingValue = "true", matchIfMissing = true)
public class ArtemisTransferenciaRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("jms:queue:{{sipap.cola.entrada}}")
                .routeId("distribuidor-artemis")
                .unmarshal().json(MensajeTransferencia.class)
                .process(this::restaurarCorrelacion)
                .choice()
                    .when(simple("${body.transferencia.merchantAccountInformation.codigoEntidad} == '0015'"))
                        .marshal().json().to("jms:queue:{{sipap.cola.itau}}")
                    .when(simple("${body.transferencia.merchantAccountInformation.codigoEntidad} == '0007'"))
                        .marshal().json().to("jms:queue:{{sipap.cola.atlas}}")
                    .when(simple("${body.transferencia.merchantAccountInformation.codigoEntidad} == '0020'"))
                        .marshal().json().to("jms:queue:{{sipap.cola.familiar}}")
                    .otherwise()
                        .process(exchange -> {
                            MensajeTransferencia mensaje = exchange.getMessage()
                                    .getBody(MensajeTransferencia.class);
                            exchange.getMessage().setBody(new ResultadoTransferencia(
                                    mensaje.transferencia().idTransaccion(), "RECHAZADA",
                                    "Código de entidad desconocido: "
                                            + mensaje.transferencia().merchantAccountInformation().codigoEntidad()));
                        })
                        .to("direct:resultados")
                .end();
    }

    private void restaurarCorrelacion(Exchange exchange) {
        MensajeTransferencia mensaje = exchange.getMessage().getBody(MensajeTransferencia.class);
        String id = mensaje.transferencia().idTransaccion();
        exchange.getMessage().setHeader("idTransaccion", id);
        exchange.getMessage().setHeader("JMSCorrelationID", id);
    }
}
