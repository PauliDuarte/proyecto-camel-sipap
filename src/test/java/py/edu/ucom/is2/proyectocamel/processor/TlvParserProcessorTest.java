package py.edu.ucom.is2.proyectocamel.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import py.edu.ucom.is2.proyectocamel.model.Transferencia;
import py.edu.ucom.is2.proyectocamel.service.GeneradorQrService;

class TlvParserProcessorTest {

    private final TlvParserProcessor parser = new TlvParserProcessor();
    private final GeneradorQrService generador = new GeneradorQrService();

    @Test
    void parseaTlvValidoYConservaId() throws Exception {
        Exchange exchange = exchange(generador.generarQrDinamico("0015", "100001", "150000", "A1B2"));

        parser.process(exchange);

        Transferencia transferencia = assertInstanceOf(Transferencia.class, exchange.getMessage().getBody());
        assertEquals("TX000001", transferencia.idTransaccion());
        assertEquals("0015", transferencia.merchantAccountInformation().codigoEntidad());
        assertEquals("100001", transferencia.merchantAccountInformation().numeroCuenta());
    }

    @Test
    void rechazaCuandoLongitudDeclaradaSuperaContenidoReal() {
        Exchange exchange = exchange("0002015909JUAN");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.process(exchange));

        assertEquals("Longitud declarada no coincide con el contenido para el tag 59", error.getMessage());
    }

    private Exchange exchange(String body) {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader("idTransaccion", "TX000001");
        exchange.getMessage().setBody(body);
        return exchange;
    }
}
