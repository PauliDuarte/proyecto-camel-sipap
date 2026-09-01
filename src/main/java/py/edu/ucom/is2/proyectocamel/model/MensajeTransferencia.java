package py.edu.ucom.is2.proyectocamel.model;

import java.math.BigDecimal;
public record MensajeTransferencia(
        Transferencia transferencia,
        String fechaTransaccion,
        BigDecimal monto) {
}
