package py.edu.ucom.is2.proyectocamel.processor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.MerchantAccountInformation;
import py.edu.ucom.is2.proyectocamel.model.Transferencia;

@Component
public class TlvParserProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String qr = exchange.getMessage().getBody(String.class);
        String idTransaccion = exchange.getMessage().getHeader("idTransaccion", String.class);

        Map<String, String> campos = parsearTlv(qr);
        Map<String, String> cuenta = parsearTlv(campos.get("32"));

        BigDecimal monto = null;
        if (campos.containsKey("54") && !campos.get("54").isBlank()) {
            try {
                monto = new BigDecimal(campos.get("54"));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("El monto no tiene un formato numérico válido", ex);
            }
        }

        Transferencia transferencia = new Transferencia(
                idTransaccion,
                campos.get("00"),
                campos.get("01"),
                new MerchantAccountInformation(cuenta.get("00"), cuenta.get("01"), cuenta.get("02")),
                campos.get("52"),
                campos.get("53"),
                monto,
                campos.get("58"),
                campos.get("59"),
                campos.get("60"),
                campos.get("63"));

        exchange.getMessage().setBody(transferencia);
    }

    public Map<String, String> parsearTlv(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido TLV está vacío");
        }

        Map<String, String> campos = new LinkedHashMap<>();
        int posicion = 0;
        while (posicion < contenido.length()) {
            if (contenido.length() - posicion < 4) {
                throw new IllegalArgumentException("Estructura TLV incompleta en la posición " + posicion);
            }

            String tag = contenido.substring(posicion, posicion + 2);
            String longitudTexto = contenido.substring(posicion + 2, posicion + 4);
            if (!tag.chars().allMatch(Character::isDigit)
                    || !longitudTexto.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("Tag o longitud TLV inválidos en la posición " + posicion);
            }

            int longitud = Integer.parseInt(longitudTexto);
            int inicioValor = posicion + 4;
            int finValor = inicioValor + longitud;
            if (finValor > contenido.length()) {
                throw new IllegalArgumentException(
                        "Longitud declarada no coincide con el contenido para el tag " + tag);
            }
            if (campos.putIfAbsent(tag, contenido.substring(inicioValor, finValor)) != null) {
                throw new IllegalArgumentException("Tag TLV duplicado: " + tag);
            }
            posicion = finValor;
        }
        return campos;
    }
}
