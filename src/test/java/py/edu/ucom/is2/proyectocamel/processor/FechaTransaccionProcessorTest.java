package py.edu.ucom.is2.proyectocamel.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import py.edu.ucom.is2.proyectocamel.model.MensajeTransferencia;
import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

class FechaTransaccionProcessorTest {

    private static final ZoneId ZONA = ZoneId.of("America/Asuncion");
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-25T15:00:00Z"), ZONA);

    @Test
    void aceptaFechaActual() throws Exception {
        assertFecha(LocalDate.of(2026, 8, 25), true);
    }

    @Test
    void rechazaFechaAnterior() throws Exception {
        assertFecha(LocalDate.of(2026, 8, 24), false);
    }

    @Test
    void rechazaFechaPosterior() throws Exception {
        assertFecha(LocalDate.of(2026, 8, 26), false);
    }

    private void assertFecha(LocalDate fecha, boolean esperado) throws Exception {
        GeneradorQrService generador = new GeneradorQrService();
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader("idTransaccion", "TX-FECHA");
        exchange.getMessage().setBody(generador.generarQrDinamico("0015", "100001", "10", "A1B2"));
        new TlvParserProcessor().process(exchange);
        Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
        exchange.getMessage().setBody(new MensajeTransferencia(
                transferencia, fecha.toString(), new BigDecimal("10")));

        new FechaTransaccionProcessor(clock).process(exchange);

        assertEquals(esperado, exchange.getMessage().getHeader("fechaValida", Boolean.class));
    }
}
