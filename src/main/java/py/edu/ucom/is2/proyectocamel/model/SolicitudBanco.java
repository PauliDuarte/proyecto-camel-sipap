package py.edu.ucom.is2.proyectocamel.model;

import java.math.BigDecimal;

public record SolicitudBanco(
        String idTransaccion,
        String codigoEntidad,
        String numeroCuenta,
        BigDecimal monto) {
}
