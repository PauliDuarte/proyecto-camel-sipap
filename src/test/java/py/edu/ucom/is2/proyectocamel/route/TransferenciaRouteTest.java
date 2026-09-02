package py.edu.ucom.is2.proyectocamel.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

import py.edu.ucom.is2.proyectocamel.model.RespuestaApiTransferencia;
import py.edu.ucom.is2.proyectocamel.model.SolicitudTransferencia;
import py.edu.ucom.is2.proyectocamel.processor.MontoProcessor;
import py.edu.ucom.is2.proyectocamel.processor.TlvParserProcessor;
import py.edu.ucom.is2.proyectocamel.processor.ValidacionProcessor;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

class TransferenciaRouteTest {

    private final GeneradorQrService generador = new GeneradorQrService();

    @Test
    void bancoDesconocidoSeRechazaAntesDeJms() throws Exception {
        probarRechazo(
                solicitud("TX-DESCONOCIDO", generador.generarQrDinamico(
                        "9999", "900009", "300000", "A1B2"), "300000"),
                "Código de entidad desconocido: 9999");
    }

    @Test
    void qrInvalidoSeRechazaAntesDeJms() throws Exception {
        probarRechazo(solicitud("TX-QR-INVALIDO", "0002015909JUAN", "10"),
                "Longitud declarada no coincide con el contenido para el tag 59");
    }

    @Test
    void solicitudValidaSePublicaYConservaId() throws Exception {
        probarPublicacion("TX-VALIDA", "150000");
    }

    @Test
    void montoIgualAlMaximoSePublica() throws Exception {
        probarPublicacion("TX-LIMITE", "10000000");
    }

    @Test
    void montoMayorAlMaximoSeRechazaAntesDeJms() throws Exception {
        probarRechazo(
                solicitud("TX-SUPERA", generador.generarQrDinamico(
                        "0015", "100001", "10000000.01", "A1B2"), "10000000.01"),
                "El monto supera máximo permitido");
    }

    private void probarPublicacion(String id, String monto) throws Exception {
        try (CamelContext context = contextoConRuta()) {
            AdviceWith.adviceWith(context, "api-transferencias", route ->
                    route.weaveByToUri("jms:queue:*").replace().to("mock:entrada"));
            MockEndpoint entrada = context.getEndpoint("mock:entrada", MockEndpoint.class);
            entrada.expectedMessageCount(1);
            entrada.expectedHeaderReceived("idTransaccion", id);
            context.start();
            try (ProducerTemplate producer = context.createProducerTemplate()) {
                Exchange exchange = producer.request("direct:api-transferencias", mensaje -> {
                    mensaje.getMessage().setHeader("JMSMessageID", "ID:interno");
                    mensaje.getMessage().setHeader("Matched-Stub-Id", "stub-interno");
                    mensaje.getMessage().setBody(solicitud(id, generador.generarQrDinamico(
                            "0015", "100001", monto, "A1B2"), monto));
                });
                RespuestaApiTransferencia respuesta = assertInstanceOf(
                        RespuestaApiTransferencia.class, exchange.getMessage().getBody());
                assertEquals(id, respuesta.idTransaccion());
                assertEquals("ACEPTADA_PARA_PROCESAMIENTO", respuesta.estado());
                assertHeadersInternosEliminados(exchange);
                MockEndpoint.assertIsSatisfied(context);
            }
        }
    }

    private void probarRechazo(SolicitudTransferencia solicitud, String mensaje) throws Exception {
        try (CamelContext context = contextoConRuta()) {
            AdviceWith.adviceWith(context, "api-transferencias", route ->
                    route.weaveByToUri("jms:queue:*").replace().to("mock:entrada"));
            MockEndpoint entrada = context.getEndpoint("mock:entrada", MockEndpoint.class);
            entrada.expectedMessageCount(0);
            context.start();
            try (ProducerTemplate producer = context.createProducerTemplate()) {
                Exchange exchange = producer.request("direct:api-transferencias", intercambio -> {
                    intercambio.getMessage().setHeader("JMSMessageID", "ID:interno");
                    intercambio.getMessage().setHeader("banco", "interno");
                    intercambio.getMessage().setBody(solicitud);
                });
                RespuestaApiTransferencia respuesta = assertInstanceOf(
                        RespuestaApiTransferencia.class, exchange.getMessage().getBody());
                assertEquals("RECHAZADA", respuesta.estado());
                assertEquals(mensaje, respuesta.mensaje());
                assertEquals(solicitud.idTransaccion(), respuesta.idTransaccion());
                assertHeadersInternosEliminados(exchange);
                MockEndpoint.assertIsSatisfied(context);
            }
        }
    }

    private CamelContext contextoConRuta() throws Exception {
        CamelContext context = new DefaultCamelContext();
        Properties properties = new Properties();
        properties.setProperty("sipap.cola.entrada", "sipap.entrada");
        context.getPropertiesComponent().setInitialProperties(properties);
        context.addRoutes(new TransferenciaRoute(
                new TlvParserProcessor(), new ValidacionProcessor(), new MontoProcessor(new BigDecimal("10000000"))));
        return context;
    }

    private SolicitudTransferencia solicitud(String id, String qr, String monto) {
        return new SolicitudTransferencia(id, LocalDate.now().toString(), qr, monto);
    }

    private void assertHeadersInternosEliminados(Exchange exchange) {
        for (String header : java.util.List.of(
                "JMSMessageID", "JMSDestination", "JMSReplyTo", "JMSCorrelationID",
                "JMSDeliveryMode", "JMSPriority", "JMSRedelivered", "JMSXDeliveryCount",
                "Matched-Stub-Id", "X-Transaction-Id", "banco", "fechaValida",
                "montoEfectivo", "montoExterno", "idTransaccion")) {
            assertFalse(exchange.getMessage().getHeaders().containsKey(header),
                    "No debe exponerse el header interno " + header);
        }
    }
}
