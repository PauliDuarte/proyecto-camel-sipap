package py.edu.ucom.is2.proyectocamel.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class GeneradorQrService {

    private final AtomicInteger contadorA = new AtomicInteger();
    private final AtomicInteger contadorB = new AtomicInteger();

    private final List<String> escenariosA = List.of(
            generarQrDinamico("0015", "100001", "150000", "A1B2"),
            generarQrDinamico("0020", "300003", "350000", "A1B2"),
            "0002010102125909JUAN",
            generarQrDinamico("0015", "100001", "450000", "FFFF"));

    private final List<String> escenariosB = List.of(
            generarQrDinamico("0007", "200002", "250000", "A1B2"),
            generarQrDinamico("9999", "900009", "300000", "A1B2"),
            generarQrDinamico("0007", "200002", "10000000", "A1B2"));

    public String siguienteProductorA() {
        return escenariosA.get(Math.floorMod(contadorA.getAndIncrement(), escenariosA.size()));
    }

    public String siguienteProductorB() {
        return escenariosB.get(Math.floorMod(contadorB.getAndIncrement(), escenariosB.size()));
    }

    public String generarQrDinamico(String codigoEntidad, String cuenta, String monto, String crc) {
        return generarQr("12", codigoEntidad, cuenta, monto, crc);
    }

    public String generarQrEstatico(String codigoEntidad, String cuenta, String crc) {
        return generarQr("11", codigoEntidad, cuenta, null, crc);
    }

    public String tlv(String tag, String valor) {
        if (valor == null || valor.length() > 99) {
            throw new IllegalArgumentException("Valor TLV inválido para el tag " + tag);
        }
        return tag + String.format("%02d", valor.length()) + valor;
    }

    private String generarQr(String metodo, String codigoEntidad, String cuenta, String monto, String crc) {
        String cuentaAnidada = tlv("00", "py.gov.bcp.sip")
                + tlv("01", codigoEntidad)
                + tlv("02", cuenta);

        String qr = tlv("00", "01")
                + tlv("01", metodo)
                + tlv("32", cuentaAnidada)
                + tlv("52", "5731")
                + tlv("53", "600");
        if (monto != null) {
            qr += tlv("54", monto);
        }
        return qr
                + tlv("58", "PY")
                + tlv("59", "JUAN PEREZ")
                + tlv("60", "ASUNCION")
                + tlv("63", crc);
    }
}
