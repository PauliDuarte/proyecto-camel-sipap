package py.edu.ucom.is2.proyectocamel.route;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.ResultadoTransferencia;
import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.processor.TlvParserProcessor;
import py.edu.ucom.is2.proyectocamel.processor.ValidacionProcessor;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

@Component
public class TransferenciaRoute extends RouteBuilder {

    private final GeneradorQrService generadorQrService;
    private final TlvParserProcessor tlvParserProcessor;
    private final ValidacionProcessor validacionProcessor;
    private final AtomicLong secuencia = new AtomicLong();

    public TransferenciaRoute(GeneradorQrService generadorQrService,
            TlvParserProcessor tlvParserProcessor,
            ValidacionProcessor validacionProcessor) {
        this.generadorQrService = generadorQrService;
        this.tlvParserProcessor = tlvParserProcessor;
        this.validacionProcessor = validacionProcessor;
    }

    @Override
    public void configure() {
        onException(IllegalArgumentException.class)
                .handled(true)
                .process(exchange -> {
                    String id = exchange.getMessage().getHeader("idTransaccion", String.class);
                    Exception causa = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    exchange.getMessage().setBody(new ResultadoTransferencia(id, "RECHAZADA", causa.getMessage()));
                })
                .to("direct:rechazados");

        from("timer:productorA?period={{sipap.productor-a.periodo}}&delay=1000")
                .routeId("productor-a")
                .setBody(exchange -> generadorQrService.siguienteProductorA())
                .log("[PRODUCTOR A] ${body}")
                .to("direct:sipap-in");

        from("timer:productorB?period={{sipap.productor-b.periodo}}&delay=2000")
                .routeId("productor-b")
                .setBody(exchange -> generadorQrService.siguienteProductorB())
                .log("[PRODUCTOR B] ${body}")
                .to("direct:sipap-in");

        from("direct:sipap-in")
                .routeId("sipap-principal")
                .process(exchange -> exchange.getMessage().setHeader(
                        "idTransaccion", String.format("TX%06d", secuencia.incrementAndGet())))
                .log("[ENTRADA SIPAP] ${header.idTransaccion} - ${body}")
                .process(tlvParserProcessor)
                .log("[PARSEO] ${header.idTransaccion} - ${body}")
                .process(validacionProcessor)
                .log("[VALIDACION] ${header.idTransaccion} - transferencia válida")
                .wireTap("direct:auditoria")
                .choice()
                    .when(simple("${body.merchantAccountInformation.codigoEntidad} == '0015'"))
                        .to("direct:itau")
                    .when(simple("${body.merchantAccountInformation.codigoEntidad} == '0007'"))
                        .to("direct:atlas")
                    .when(simple("${body.merchantAccountInformation.codigoEntidad} == '0020'"))
                        .to("direct:familiar")
                    .otherwise()
                        .process(exchange -> {
                            Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
                            exchange.getMessage().setBody(new ResultadoTransferencia(
                                    transferencia.idTransaccion(), "RECHAZADA",
                                    "Código de entidad desconocido: "
                                            + transferencia.merchantAccountInformation().codigoEntidad()));
                        })
                        .to("direct:rechazados")
                .end();

        consumidorBanco("direct:itau", "ITAU");
        consumidorBanco("direct:atlas", "ATLAS");
        consumidorBanco("direct:familiar", "FAMILIAR");

        from("direct:rechazados")
                .routeId("transferencias-rechazadas")
                .log("[RECHAZADA] ${body}");

        from("direct:auditoria")
                .routeId("auditoria")
                .log("[AUDITORIA] ${header.idTransaccion} - ${body}");
    }

    private void consumidorBanco(String endpoint, String banco) {
        from(endpoint)
                .routeId("banco-" + banco.toLowerCase())
                .log("[" + banco + "] Modelo recibido: ${body}")
                .process(exchange -> {
                    Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
                    exchange.getMessage().setBody(new ResultadoTransferencia(
                            transferencia.idTransaccion(), "PROCESADA",
                            "Transferencia procesada exitosamente"));
                })
                .log("[" + banco + "] Resultado: ${body}");
    }
}
