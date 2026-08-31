package py.edu.ucom.is2.proyectocamel.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

class MontoProcessorTest {

    private final MontoProcessor processor = new MontoProcessor(new BigDecimal("10000000"));
    private final TlvParserProcessor parser = new TlvParserProcessor();
    private final GeneradorQrService generador = new GeneradorQrService();

    @Test
    void permiteMontoIgualAlMaximo() throws Exception {
        Exchange exchange = exchangeDinamico("10000000", "10000000");
        assertDoesNotThrow(() -> processor.process(exchange));
        assertEquals(new BigDecimal("10000000"),
                exchange.getMessage().getBody(Transferencia.class).transactionAmount());
    }

    @Test
    void rechazaMontoMayorAlMaximoConMensajeExacto() throws Exception {
        Exchange exchange = exchangeDinamico("10000000.01", "10000000.01");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> processor.process(exchange));
        assertEquals("El monto supera máximo permitido", error.getMessage());
    }

    @Test
    void qrEstaticoUsaMontoExterno() throws Exception {
        Exchange exchange = parsear(generador.generarQrEstatico("0015", "100001", "A1B2"));
        exchange.getMessage().setHeader("montoExterno", "150000");
        processor.process(exchange);
        assertEquals(new BigDecimal("150000"),
                exchange.getMessage().getBody(Transferencia.class).transactionAmount());
    }

    @Test
    void rechazaInconsistenciaEntreMontoDinamicoYExterno() throws Exception {
        Exchange exchange = exchangeDinamico("150000", "150001");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> processor.process(exchange));
        assertEquals("El monto externo no coincide con el monto del QR dinámico", error.getMessage());
    }

    private Exchange exchangeDinamico(String montoQr, String montoExterno) throws Exception {
        Exchange exchange = parsear(generador.generarQrDinamico("0015", "100001", montoQr, "A1B2"));
        exchange.getMessage().setHeader("montoExterno", montoExterno);
        return exchange;
    }

    private Exchange parsear(String qr) throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader("idTransaccion", "TX-MONTO");
        exchange.getMessage().setBody(qr);
        parser.process(exchange);
        return exchange;
    }
}
