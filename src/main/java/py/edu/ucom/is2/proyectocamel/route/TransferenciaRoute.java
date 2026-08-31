package py.edu.ucom.is2.proyectocamel.route;

import java.time.LocalDate;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.MensajeTransferencia;
import py.edu.ucom.is2.proyectocamel.model.RespuestaApiTransferencia;
import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.processor.MontoProcessor;
import py.edu.ucom.is2.proyectocamel.processor.TlvParserProcessor;
import py.edu.ucom.is2.proyectocamel.processor.ValidacionProcessor;

@Component
public class TransferenciaRoute extends RouteBuilder {

    private final TlvParserProcessor tlvParserProcessor;
    private final ValidacionProcessor validacionProcessor;
    private final MontoProcessor montoProcessor;

    public TransferenciaRoute(TlvParserProcessor tlvParserProcessor,
            ValidacionProcessor validacionProcessor,
            MontoProcessor montoProcessor) {
        this.tlvParserProcessor = tlvParserProcessor;
        this.validacionProcessor = validacionProcessor;
        this.montoProcessor = montoProcessor;
    }

    @Override
    public void configure() {
        from("direct:api-transferencias")
                .routeId("api-transferencias")
                .doTry()
                    .process(this::prepararSolicitud)
                    .process(tlvParserProcessor)
                    .process(validacionProcessor)
                    .process(montoProcessor)
                    .process(this::validarBancoConocido)
                    .wireTap("direct:auditoria")
                    .process(this::construirMensaje)
                    .marshal().json()
                    .to("jms:queue:{{sipap.cola.entrada}}")
                    .process(exchange -> {
                        String id = exchange.getMessage().getHeader("idTransaccion", String.class);
                        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 202);
                        exchange.getMessage().setBody(new RespuestaApiTransferencia(
                                id, "ACEPTADA_PARA_PROCESAMIENTO",
                                "Transferencia publicada para procesamiento"));
                    })
                .endDoTry()
                .doCatch(Exception.class)
                    .process(exchange -> {
                        String id = exchange.getMessage().getHeader("idTransaccion", String.class);
                        Exception causa = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                        exchange.getMessage().setBody(new RespuestaApiTransferencia(
                                id, "RECHAZADA", mensajeError(causa)));
                    })
                    .to("direct:rechazados")
                .end();

        from("direct:rechazados")
                .routeId("transferencias-rechazadas")
                .log("[RECHAZADA] ${body}");

        from("direct:auditoria")
                .routeId("auditoria")
                .log("[AUDITORIA] ${header.idTransaccion} - ${body}");
    }

    private void prepararSolicitud(Exchange exchange) {
        py.edu.ucom.is2.proyectocamel.model.SolicitudTransferencia solicitud = exchange.getMessage()
                .getBody(py.edu.ucom.is2.proyectocamel.model.SolicitudTransferencia.class);
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }
        obligatorio(solicitud.idTransaccion(), "id_transaccion");
        obligatorio(solicitud.fechaTransaccion(), "fecha_transaccion");
        obligatorio(solicitud.qr(), "qr");

        LocalDate fecha = LocalDate.parse(solicitud.fechaTransaccion());
        exchange.getMessage().setHeader("idTransaccion", solicitud.idTransaccion());
        exchange.getMessage().setHeader("fechaTransaccion", fecha);
        exchange.getMessage().setHeader("montoExterno", solicitud.monto());
        exchange.getMessage().setHeader("JMSCorrelationID", solicitud.idTransaccion());
        exchange.getMessage().setBody(solicitud.qr());
    }

    private void validarBancoConocido(Exchange exchange) {
        Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
        String codigo = transferencia.merchantAccountInformation().codigoEntidad();
        if (!"0015".equals(codigo) && !"0007".equals(codigo) && !"0020".equals(codigo)) {
            throw new IllegalArgumentException("Código de entidad desconocido: " + codigo);
        }
    }

    private void construirMensaje(Exchange exchange) {
        Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
        LocalDate fecha = exchange.getMessage().getHeader("fechaTransaccion", LocalDate.class);
        exchange.getMessage().setBody(new MensajeTransferencia(
                transferencia, fecha.toString(), transferencia.transactionAmount()));
    }

    private void obligatorio(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el campo obligatorio " + nombre);
        }
    }

    private String mensajeError(Exception causa) {
        if (causa == null) {
            return "Error al procesar la transferencia";
        }
        return causa.getMessage() == null ? causa.getClass().getSimpleName() : causa.getMessage();
    }
}
