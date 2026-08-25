# Checklist de entrega

Revisión realizada contra el código, las pruebas y la ejecución de la rama `rama_steven`.

## Funcionalidad

- [x] **Al menos dos productores:** existen las rutas `productor-a` y `productor-b`.
- [x] **`timer:`:** ambos productores se originan en endpoints `timer:`.
- [x] **`direct:sipap-in`:** los dos productores convergen en este canal interno.
- [x] **Parser TLV:** `TlvParserProcessor` interpreta tag, longitud y valor.
- [x] **TLV anidado tag 32:** el valor del tag `32` vuelve a procesarse con `parsearTlv`.
- [x] **Transformación a modelo canónico:** el parser construye `Transferencia` y `MerchantAccountInformation`.
- [x] **Validaciones:** `ValidacionProcessor` aplica las reglas generales requeridas.
- [x] **Enrutamiento ITAU:** código `0015` hacia `direct:itau`.
- [x] **Enrutamiento ATLAS:** código `0007` hacia `direct:atlas`.
- [x] **Enrutamiento FAMILIAR:** código `0020` hacia `direct:familiar`.
- [x] **Rechazados:** errores y bancos desconocidos terminan en `direct:rechazados`.
- [x] **ID de transacción:** se asigna con formato `TX%06d` y se conserva en los resultados.
- [x] **Consumidores reciben `Transferencia`:** los tres consumidores convierten el body desde `Transferencia.class`; la ejecución muestra `Modelo recibido: Transferencia[...]`.

## Validaciones

- [x] **Campos obligatorios:** se comprueban mediante el método `obligatorio`.
- [x] **Longitudes TLV:** el parser compara el final declarado con la longitud real.
- [x] **Payload `01`:** cualquier otro valor se rechaza.
- [x] **Método `11`/`12`:** solo ambos valores son aceptados.
- [x] **GUID `py.gov.bcp.sip`:** se valida exactamente.
- [x] **Cuenta desde sub-tag `02`:** el parser toma `cuenta.get("02")`.
- [x] **Moneda `600`:** se valida exactamente.
- [x] **Monto obligatorio para dinámico:** es obligatorio cuando el método es `12`.
- [x] **Monto positivo:** valores menores o iguales a cero se rechazan cuando el monto está presente.
- [x] **Monto `>= 10000000` rechazado:** la comparación implementada incluye la igualdad.
- [x] **CRC `A1B2`:** se valida exactamente.
- [x] **Banco desconocido rechazado:** `choice()/otherwise` crea un resultado rechazado; no lo hace `ValidacionProcessor`.

## EIP

- [x] **Mínimo cinco patrones realmente implementados:** se verificaron siete: Message Channel, Pipes and Filters, Message Translator, Content-Based Router, Correlation Identifier, manejo de errores equivalente y Wire Tap.
- [x] **No contar patrones ausentes:** no se cuenta Message Filter porque no existe una construcción `.filter()` en la ruta. Las excepciones y `choice()/otherwise` producen un efecto de exclusión, pero no constituyen un Message Filter explícito.

## Entrega

- [x] **Código fuente completo:** están presentes la aplicación, modelos, processors, servicio y ruta.
- [x] **README:** `README.md` documenta objetivo, flujo, formato, validaciones y restricciones.
- [x] **Requisitos de instalación y ejecución:** README incluye versiones y comandos con Maven Wrapper.
- [x] **Rutas Camel Java DSL:** `TransferenciaRoute` extiende `RouteBuilder`.
- [x] **Parser:** existe `TlvParserProcessor`.
- [x] **Clases de dominio:** existen `Transferencia`, `MerchantAccountInformation` y `ResultadoTransferencia`.
- [x] **Al menos dos consumidores:** existen tres consumidores bancarios.
- [x] **Diagrama:** README contiene un diagrama Mermaid.
- [x] **Ejemplos QR válidos e inválidos:** README contiene ejemplos generados por `GeneradorQrService`.
- [x] **Evidencia de ejecución:** disponible en `docs/evidencias.md`.
- [x] **Tests:** 11 tests ejecutados sin failures, errors ni skipped.
