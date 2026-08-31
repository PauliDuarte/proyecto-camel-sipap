package py.edu.ucom.is2.proyectocamel.processor;

import java.time.Clock;
import java.time.LocalDate;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.MensajeTransferencia;

@Component
public class FechaTransaccionProcessor implements Processor {

    private final Clock clock;

    public FechaTransaccionProcessor(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void process(Exchange exchange) {
        MensajeTransferencia mensaje = exchange.getMessage().getBody(MensajeTransferencia.class);
        boolean fechaValida = false;
        if (mensaje != null && mensaje.fechaTransaccion() != null) {
            try {
                fechaValida = LocalDate.parse(mensaje.fechaTransaccion()).equals(LocalDate.now(clock));
            } catch (java.time.format.DateTimeParseException ex) {
                fechaValida = false;
            }
        }
        exchange.getMessage().setHeader("fechaValida", fechaValida);
    }
}
