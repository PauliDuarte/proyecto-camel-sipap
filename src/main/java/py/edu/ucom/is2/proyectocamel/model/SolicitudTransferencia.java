package py.edu.ucom.is2.proyectocamel.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SolicitudTransferencia(
        @JsonProperty("id_transaccion") String idTransaccion,
        @JsonProperty("fecha_transaccion") String fechaTransaccion,
        String qr,
        String monto) {
}
