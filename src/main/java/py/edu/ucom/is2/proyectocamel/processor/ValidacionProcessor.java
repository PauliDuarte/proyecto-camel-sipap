package py.edu.ucom.is2.proyectocamel.processor;

import java.math.BigDecimal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.MerchantAccountInformation;
import py.edu.ucom.is2.proyectocamel.model.Transferencia;

@Component
public class ValidacionProcessor implements Processor {

    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("10000000");

    @Override
    public void process(Exchange exchange) {
        Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
        validar(transferencia);
    }

    public void validar(Transferencia transferencia) {
        if (transferencia == null) {
            throw new IllegalArgumentException("No existe una transferencia para validar");
        }
        obligatorio(transferencia.idTransaccion(), "idTransaccion");
        obligatorio(transferencia.payloadFormatIndicator(), "payloadFormatIndicator");
        obligatorio(transferencia.pointOfInitiationMethod(), "pointOfInitiationMethod");
        obligatorio(transferencia.merchantCategoryCode(), "merchantCategoryCode");
        obligatorio(transferencia.transactionCurrency(), "transactionCurrency");
        obligatorio(transferencia.countryCode(), "countryCode");
        obligatorio(transferencia.merchantName(), "merchantName");
        obligatorio(transferencia.merchantCity(), "merchantCity");
        obligatorio(transferencia.crc(), "crc");

        MerchantAccountInformation cuenta = transferencia.merchantAccountInformation();
        if (cuenta == null) {
            throw new IllegalArgumentException("Falta el campo obligatorio merchantAccountInformation");
        }
        obligatorio(cuenta.globallyUniqueIdentifier(), "globallyUniqueIdentifier");
        obligatorio(cuenta.codigoEntidad(), "codigoEntidad");
        obligatorio(cuenta.numeroCuenta(), "numeroCuenta");

        if (!"01".equals(transferencia.payloadFormatIndicator())) {
            throw new IllegalArgumentException("payloadFormatIndicator debe ser 01");
        }
        if (!"11".equals(transferencia.pointOfInitiationMethod())
                && !"12".equals(transferencia.pointOfInitiationMethod())) {
            throw new IllegalArgumentException("pointOfInitiationMethod debe ser 11 o 12");
        }
        if (!"py.gov.bcp.sip".equals(cuenta.globallyUniqueIdentifier())) {
            throw new IllegalArgumentException("globallyUniqueIdentifier no corresponde a SIPAP");
        }
        if (!"600".equals(transferencia.transactionCurrency())) {
            throw new IllegalArgumentException("transactionCurrency debe ser 600");
        }
        if ("12".equals(transferencia.pointOfInitiationMethod()) && transferencia.transactionAmount() == null) {
            throw new IllegalArgumentException("transactionAmount es obligatorio para QR dinámico");
        }
        if (transferencia.transactionAmount() != null) {
            if (transferencia.transactionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("transactionAmount debe ser positivo");
            }
            if (transferencia.transactionAmount().compareTo(MONTO_MAXIMO) >= 0) {
                throw new IllegalArgumentException("transactionAmount debe ser menor a 10000000");
            }
        }
        if (!"A1B2".equals(transferencia.crc())) {
            throw new IllegalArgumentException("CRC inválido: se esperaba A1B2");
        }
    }

    private void obligatorio(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el campo obligatorio " + nombre);
        }
    }
}
