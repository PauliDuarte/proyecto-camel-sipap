package py.edu.ucom.is2.proyectocamel.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.UnmarshalDefinition;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.junit.jupiter.api.Test;

import py.edu.ucom.is2.proyectocamel.model.MensajeTransferencia;
import py.edu.ucom.is2.proyectocamel.model.ResultadoTransferencia;
import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.processor.FechaTransaccionProcessor;
import py.edu.ucom.is2.proyectocamel.processor.TlvParserProcessor;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

class BancoConsumidorRouteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 25);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-25T15:00:00Z"), ZoneId.of("America/Asuncion"));

    @Test
    void respuesta200ProduceProcesadaYConservaId() throws Exception {
        ResultadoTransferencia resultado = ejecutarUnaVez(200, mensaje("TX-200", HOY, "100001"));
        assertEquals("TX-200", resultado.idTransaccion());
        assertEquals("PROCESADA", resultado.estado());
    }

    @Test
    void respuesta422ProduceRechazoBancario() throws Exception {
        ResultadoTransferencia resultado = ejecutarUnaVez(422, mensaje("TX-422", HOY, "888888"));
        assertEquals("RECHAZADA", resultado.estado());
    }

    @Test
    void respuesta500ProduceErrorBanco() throws Exception {
        ResultadoTransferencia resultado = ejecutarUnaVez(500, mensaje("TX-500", HOY, "999999"));
        assertEquals("ERROR_BANCO", resultado.estado());
    }

    @Test
    void fechaAnteriorYPosteriorNoInvocanBanco() throws Exception {
        try (CamelContext context = contextoPreparado(200)) {
            MockEndpoint banco = context.getEndpoint("mock:banco", MockEndpoint.class);
            MockEndpoint resultados = context.getEndpoint("mock:resultados", MockEndpoint.class);
            banco.expectedMessageCount(0);
            resultados.expectedMessageCount(2);
            context.start();
            try (ProducerTemplate producer = context.createProducerTemplate()) {
                producer.sendBody("direct:itau-test", mensaje("TX-AYER", HOY.minusDays(1), "100001"));
                producer.sendBody("direct:itau-test", mensaje("TX-MANANA", HOY.plusDays(1), "100001"));
                MockEndpoint.assertIsSatisfied(context);
                List<Exchange> recibidos = resultados.getReceivedExchanges();
                assertEquals("RECHAZADA_FECHA",
                        recibidos.get(0).getMessage().getBody(ResultadoTransferencia.class).estado());
                assertEquals("RECHAZADA_FECHA",
                        recibidos.get(1).getMessage().getBody(ResultadoTransferencia.class).estado());
            }
        }
    }

    @Test
    void duplicadoInvocaBancoUnaSolaVezYRegistraDuplicada() throws Exception {
        try (CamelContext context = contextoPreparado(200)) {
            MockEndpoint banco = context.getEndpoint("mock:banco", MockEndpoint.class);
            MockEndpoint resultados = context.getEndpoint("mock:resultados", MockEndpoint.class);
            banco.expectedMessageCount(1);
            resultados.expectedMessageCount(2);
            context.start();
            try (ProducerTemplate producer = context.createProducerTemplate()) {
                MensajeTransferencia mensaje = mensaje("TX-DUPLICADA", HOY, "100001");
                producer.sendBody("direct:itau-test", mensaje);
                producer.sendBody("direct:itau-test", mensaje);
                MockEndpoint.assertIsSatisfied(context);
                assertEquals(1, banco.getReceivedCounter());
                ResultadoTransferencia duplicada = resultados.getReceivedExchanges().get(1)
                        .getMessage().getBody(ResultadoTransferencia.class);
                assertEquals("DUPLICADA", duplicada.estado());
                assertEquals("La transferencia ya fue procesada", duplicada.mensaje());
            }
        }
    }

    private ResultadoTransferencia ejecutarUnaVez(int codigoHttp, MensajeTransferencia mensaje) throws Exception {
        try (CamelContext context = contextoPreparado(codigoHttp)) {
            MockEndpoint banco = context.getEndpoint("mock:banco", MockEndpoint.class);
            MockEndpoint resultados = context.getEndpoint("mock:resultados", MockEndpoint.class);
            banco.expectedMessageCount(1);
            banco.expectedHeaderReceived("X-Transaction-Id", mensaje.transferencia().idTransaccion());
            resultados.expectedMessageCount(1);
            context.start();
            try (ProducerTemplate producer = context.createProducerTemplate()) {
                producer.sendBody("direct:itau-test", mensaje);
                MockEndpoint.assertIsSatisfied(context);
                return resultados.getReceivedExchanges().getFirst()
                        .getMessage().getBody(ResultadoTransferencia.class);
            }
        }
    }

    private CamelContext contextoPreparado(int codigoHttp) throws Exception {
        CamelContext context = new DefaultCamelContext();
        Properties properties = new Properties();
        properties.setProperty("sipap.cola.itau", "sipap.itau");
        properties.setProperty("sipap.cola.atlas", "sipap.atlas");
        properties.setProperty("sipap.cola.familiar", "sipap.familiar");
        properties.setProperty("sipap.banco-mock.base-url", "http://localhost:8081");
        context.getPropertiesComponent().setInitialProperties(properties);
        context.addRoutes(new BancoConsumidorRoute(
                new FechaTransaccionProcessor(CLOCK),
                MemoryIdempotentRepository.memoryIdempotentRepository(100)));

        AdviceWith.adviceWith(context, "consumidor-itau", route -> {
            route.replaceFromWith("direct:itau-test");
            route.weaveByType(UnmarshalDefinition.class).replace().process(exchange -> { });
        });
        AdviceWith.adviceWith(context, "consumidor-atlas", route ->
                route.replaceFromWith("direct:atlas-test"));
        AdviceWith.adviceWith(context, "consumidor-familiar", route ->
                route.replaceFromWith("direct:familiar-test"));
        AdviceWith.adviceWith(context, "invocar-banco-mock", route ->
                route.weaveById("invocar-banco-http").replace()
                        .to("mock:banco")
                        .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(codigoHttp));
        AdviceWith.adviceWith(context, "resultados-transferencias", route ->
                route.weaveAddLast().to("mock:resultados"));
        return context;
    }

    private MensajeTransferencia mensaje(String id, LocalDate fecha, String cuenta) throws Exception {
        GeneradorQrService generador = new GeneradorQrService();
        Exchange exchange = new org.apache.camel.support.DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader("idTransaccion", id);
        exchange.getMessage().setBody(generador.generarQrDinamico("0015", cuenta, "150000", "A1B2"));
        new TlvParserProcessor().process(exchange);
        Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
        return new MensajeTransferencia(transferencia, fecha.toString(), new BigDecimal("150000"));
    }
}
