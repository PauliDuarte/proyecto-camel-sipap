package py.edu.ucom.is2.proyectocamel.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.MensajeTransferencia;
import py.edu.ucom.is2.proyectocamel.model.ResultadoTransferencia;
import py.edu.ucom.is2.proyectocamel.model.SolicitudBanco;
import py.edu.ucom.is2.proyectocamel.processor.FechaTransaccionProcessor;

@Component
@ConditionalOnProperty(name = "sipap.jms.habilitado", havingValue = "true", matchIfMissing = true)
public class BancoConsumidorRoute extends RouteBuilder {

    private final FechaTransaccionProcessor fechaProcessor;
    private final IdempotentRepository repositorio;

    public BancoConsumidorRoute(FechaTransaccionProcessor fechaProcessor,
            @Qualifier("transferenciasIdempotentRepository") IdempotentRepository repositorio) {
        this.fechaProcessor = fechaProcessor;
        this.repositorio = repositorio;
    }

    @Override
    public void configure() {
        consumidor("jms:queue:{{sipap.cola.itau}}", "itau");
        consumidor("jms:queue:{{sipap.cola.atlas}}", "atlas");
        consumidor("jms:queue:{{sipap.cola.familiar}}", "familiar");

        from("direct:invocar-banco")
                .routeId("invocar-banco-mock")
                .process(this::prepararSolicitudBanco)
                .marshal().json()
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("X-Transaction-Id", header("idTransaccion"))
                .toD("{{sipap.banco-mock.base-url}}/bancos/${header.banco}/transferencias"
                        + "?throwExceptionOnFailure=false").id("invocar-banco-http")
                .process(this::procesarRespuestaBanco)
                .to("direct:resultados");

        from("direct:resultados")
                .routeId("resultados-transferencias")
                .log("[RESULTADO] ${body}");
    }

    private void consumidor(String cola, String banco) {
        from(cola)
                .routeId("consumidor-" + banco)
                .unmarshal().json(MensajeTransferencia.class)
                .setHeader("banco", constant(banco))
                .process(this::restaurarCorrelacion)
                .process(fechaProcessor)
                .choice()
                    .when(header("fechaValida").isEqualTo(false))
                        .process(exchange -> resultado(exchange, "RECHAZADA_FECHA",
                                "La fecha de transacción no coincide con la fecha actual"))
                        .to("direct:resultados")
                    .otherwise()
                        .idempotentConsumer(header("idTransaccion"), repositorio)
                            .skipDuplicate(false)
                            .choice()
                                .when(exchangeProperty("CamelDuplicateMessage").isEqualTo(true))
                                    .process(exchange -> resultado(exchange, "DUPLICADA",
                                            "La transferencia ya fue procesada"))
                                    .to("direct:resultados")
                                .otherwise()
                                    .to("direct:invocar-banco")
                            .end()
                        .end()
                .end();
    }

    private void restaurarCorrelacion(Exchange exchange) {
        MensajeTransferencia mensaje = exchange.getMessage().getBody(MensajeTransferencia.class);
        String id = mensaje.transferencia().idTransaccion();
        exchange.getMessage().setHeader("idTransaccion", id);
        exchange.getMessage().setHeader("JMSCorrelationID", id);
    }

    private void prepararSolicitudBanco(Exchange exchange) {
        MensajeTransferencia mensaje = exchange.getMessage().getBody(MensajeTransferencia.class);
        exchange.getMessage().setBody(new SolicitudBanco(
                mensaje.transferencia().idTransaccion(),
                mensaje.transferencia().merchantAccountInformation().codigoEntidad(),
                mensaje.transferencia().merchantAccountInformation().numeroCuenta(),
                mensaje.monto()));
    }

    private void procesarRespuestaBanco(Exchange exchange) {
        String id = exchange.getMessage().getHeader("idTransaccion", String.class);
        Integer codigo = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (codigo != null && codigo >= 200 && codigo < 300) {
            exchange.getMessage().setBody(new ResultadoTransferencia(
                    id, "PROCESADA", "Transferencia procesada por el banco"));
        } else if (codigo != null && codigo == 422) {
            exchange.getMessage().setBody(new ResultadoTransferencia(
                    id, "RECHAZADA", "Transferencia rechazada por el banco"));
        } else {
            exchange.getMessage().setBody(new ResultadoTransferencia(
                    id, "ERROR_BANCO", "Error al invocar el banco"));
        }
    }

    private void resultado(Exchange exchange, String estado, String mensajeResultado) {
        MensajeTransferencia mensaje = exchange.getMessage().getBody(MensajeTransferencia.class);
        exchange.getMessage().setBody(new ResultadoTransferencia(
                mensaje.transferencia().idTransaccion(), estado, mensajeResultado));
    }
}
