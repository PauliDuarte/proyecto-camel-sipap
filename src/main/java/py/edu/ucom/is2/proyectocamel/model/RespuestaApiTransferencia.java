package py.edu.ucom.is2.proyectocamel.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RespuestaApiTransferencia(
        @JsonProperty("id_transaccion") String idTransaccion,
        String estado,
        String mensaje) {
}
