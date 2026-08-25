package py.edu.ucom.is2.proyectocamel.model;

import java.math.BigDecimal;

public record Transferencia(
        String idTransaccion,
        String payloadFormatIndicator,
        String pointOfInitiationMethod,
        MerchantAccountInformation merchantAccountInformation,
        String merchantCategoryCode,
        String transactionCurrency,
        BigDecimal transactionAmount,
        String countryCode,
        String merchantName,
        String merchantCity,
        String crc) {
}
