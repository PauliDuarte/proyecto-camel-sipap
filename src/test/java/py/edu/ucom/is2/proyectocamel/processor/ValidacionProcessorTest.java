package py.edu.ucom.is2.proyectocamel.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

class ValidacionProcessorTest {

    private final TlvParserProcessor parser = new TlvParserProcessor();
    private final ValidacionProcessor validador = new ValidacionProcessor();
    private final GeneradorQrService generador = new GeneradorQrService();

    @Test
    void permiteBancoDesconocidoParaQueLaRutaDecida() throws Exception {
        Transferencia transferencia = parsear(generador.generarQrDinamico("9999", "900009", "300000", "A1B2"));

        assertDoesNotThrow(() -> validador.validar(transferencia));
    }

    @Test
    void rechazaMontoIgualAlLimite() throws Exception {
        Transferencia transferencia = parsear(generador.generarQrDinamico("0007", "200002", "10000000", "A1B2"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validador.validar(transferencia));

        assertEquals("transactionAmount debe ser menor a 10000000", error.getMessage());
    }

    @Test
    void rechazaCrcIncorrecto() throws Exception {
        Transferencia transferencia = parsear(generador.generarQrDinamico("0015", "100001", "1000", "FFFF"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validador.validar(transferencia));

        assertEquals("CRC inválido: se esperaba A1B2", error.getMessage());
    }

    @Test
    void permiteQrEstaticoSinMonto() throws Exception {
        Transferencia transferencia = parsear(generador.generarQrEstatico("0015", "100001", "A1B2"));

        assertDoesNotThrow(() -> validador.validar(transferencia));
        assertEquals(null, transferencia.transactionAmount());
    }

    private Transferencia parsear(String qr) throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader("idTransaccion", "TX000001");
        exchange.getMessage().setBody(qr);
        parser.process(exchange);
        return exchange.getMessage().getBody(Transferencia.class);
    }
}
