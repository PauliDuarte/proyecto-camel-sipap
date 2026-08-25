package py.edu.ucom.is2.proyectocamel.route;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

import py.edu.ucom.is2.proyectocamel.processor.TlvParserProcessor;
import py.edu.ucom.is2.proyectocamel.processor.ValidacionProcessor;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

class TransferenciaRouteTest {

    @Test
    void bancoDesconocidoTerminaEnRechazadosYNoEnConsumidoresBancarios() throws Exception {
        GeneradorQrService generador = new GeneradorQrService();
        CamelContext context = new DefaultCamelContext();
        context.getPropertiesComponent().setInitialProperties(timerProperties());
        context.addRoutes(new TransferenciaRoute(generador, new TlvParserProcessor(), new ValidacionProcessor()));

        AdviceWith.adviceWith(context, "productor-a", route -> route.replaceFromWith("direct:productor-a-test"));
        AdviceWith.adviceWith(context, "productor-b", route -> route.replaceFromWith("direct:productor-b-test"));
        AdviceWith.adviceWith(context, "sipap-principal", route -> {
            route.interceptSendToEndpoint("direct:rechazados").skipSendToOriginalEndpoint().to("mock:rechazados");
            route.interceptSendToEndpoint("direct:itau").skipSendToOriginalEndpoint().to("mock:itau");
            route.interceptSendToEndpoint("direct:atlas").skipSendToOriginalEndpoint().to("mock:atlas");
            route.interceptSendToEndpoint("direct:familiar").skipSendToOriginalEndpoint().to("mock:familiar");
        });

        MockEndpoint rechazados = context.getEndpoint("mock:rechazados", MockEndpoint.class);
        MockEndpoint itau = context.getEndpoint("mock:itau", MockEndpoint.class);
        MockEndpoint atlas = context.getEndpoint("mock:atlas", MockEndpoint.class);
        MockEndpoint familiar = context.getEndpoint("mock:familiar", MockEndpoint.class);
        rechazados.expectedMessageCount(1);
        rechazados.message(0).body().isInstanceOf(py.edu.ucom.is2.proyectocamel.model.ResultadoTransferencia.class);
        itau.expectedMessageCount(0);
        atlas.expectedMessageCount(0);
        familiar.expectedMessageCount(0);

        context.start();
        try (ProducerTemplate producer = context.createProducerTemplate()) {
            producer.sendBody("direct:sipap-in",
                    generador.generarQrDinamico("9999", "900009", "300000", "A1B2"));
            MockEndpoint.assertIsSatisfied(context);
        } finally {
            context.stop();
        }
    }

    private java.util.Properties timerProperties() {
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("sipap.productor-a.periodo", "60000");
        properties.setProperty("sipap.productor-b.periodo", "60000");
        return properties;
    }
}
