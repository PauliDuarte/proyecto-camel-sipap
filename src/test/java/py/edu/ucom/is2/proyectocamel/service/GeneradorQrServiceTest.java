package py.edu.ucom.is2.proyectocamel.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.processor.TlvParserProcessor;
import py.edu.ucom.is2.proyectocamel.processor.ValidacionProcessor;

class GeneradorQrServiceTest {

    private final GeneradorQrService generador = new GeneradorQrService();
    private final TlvParserProcessor parser = new TlvParserProcessor();
    private final ValidacionProcessor validador = new ValidacionProcessor();

    @ParameterizedTest
    @CsvSource({
            "0015,100001",
            "0007,200002",
            "0020,300003"
    })
    void generaQrValidoParaCadaBanco(String codigoEntidad, String cuenta) throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader("idTransaccion", "TX000001");
        exchange.getMessage().setBody(generador.generarQrDinamico(codigoEntidad, cuenta, "150000", "A1B2"));

        parser.process(exchange);
        Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);

        assertDoesNotThrow(() -> validador.validar(transferencia));
        assertEquals(codigoEntidad, transferencia.merchantAccountInformation().codigoEntidad());
        assertEquals(cuenta, transferencia.merchantAccountInformation().numeroCuenta());
    }
}
