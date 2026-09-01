package py.edu.ucom.is2.proyectocamel.processor;

import java.math.BigDecimal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.Transferencia;

@Component
public class MontoProcessor implements Processor {

    private final BigDecimal montoMaximo;

    public MontoProcessor(@Value("${sipap.monto-maximo:10000000}") BigDecimal montoMaximo) {
        this.montoMaximo = montoMaximo;
    }

    @Override
    public void process(Exchange exchange) {
        Transferencia transferencia = exchange.getMessage().getBody(Transferencia.class);
        String montoTexto = exchange.getMessage().getHeader("montoExterno", String.class);
        BigDecimal montoExterno = convertirMonto(montoTexto);

        BigDecimal montoEfectivo;
        if ("11".equals(transferencia.pointOfInitiationMethod())) {
            montoEfectivo = montoExterno;
        } else {
            montoEfectivo = transferencia.transactionAmount();
            if (montoExterno.compareTo(montoEfectivo) != 0) {
                throw new IllegalArgumentException("El monto externo no coincide con el monto del QR dinámico");
            }
        }

        if (montoEfectivo.compareTo(montoMaximo) > 0) {
            throw new IllegalArgumentException("El monto supera máximo permitido");
        }

        Transferencia actualizada = new Transferencia(
                transferencia.idTransaccion(),
                transferencia.payloadFormatIndicator(),
                transferencia.pointOfInitiationMethod(),
                transferencia.merchantAccountInformation(),
                transferencia.merchantCategoryCode(),
                transferencia.transactionCurrency(),
                montoEfectivo,
                transferencia.countryCode(),
                transferencia.merchantName(),
                transferencia.merchantCity(),
                transferencia.crc());
        exchange.getMessage().setBody(actualizada);
        exchange.getMessage().setHeader("montoEfectivo", montoEfectivo);
    }

    private BigDecimal convertirMonto(String montoTexto) {
        if (montoTexto == null || montoTexto.isBlank()) {
            throw new IllegalArgumentException("El monto externo es obligatorio");
        }
        try {
            BigDecimal monto = new BigDecimal(montoTexto);
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto externo debe ser positivo");
            }
            return monto;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El monto externo no tiene un formato numérico válido", ex);
        }
    }
}
