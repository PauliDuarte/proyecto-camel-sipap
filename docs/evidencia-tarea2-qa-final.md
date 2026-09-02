# Evidencia Tarea 2 - QA final

## Caso 1 - Monto límite permitido

Se probó una transferencia con monto exactamente igual a:

`10000000`

ID:

`TX-QA-LIMITE-001`

Resultado:

- HTTP `202`
- Estado: `ACEPTADA_PARA_PROCESAMIENTO`
- Mensaje: `Transferencia publicada para procesamiento`

Esto confirma que el límite de 10.000.000 PYG es inclusivo.

---

## Caso 2 - QR inválido

Se envió una transferencia con CRC distinto al valor esperado.

ID:

`TX-QA-QR-INVALIDO-001`

Resultado:

- HTTP `400`
- Estado: `RECHAZADA`
- Mensaje: `CRC inválido: se esperaba A1B2`

Esto confirma que un QR inválido es rechazado antes de continuar con el procesamiento.

---

## Caso 3 - Banco desconocido

Se envió una transferencia con código de entidad:

`9998`

ID:

`TX-QA-BANCO-001`

Resultado:

- HTTP `400`
- Estado: `RECHAZADA`
- Mensaje: `Código de entidad desconocido: 9998`

Esto confirma que solamente se aceptan los bancos configurados en la solución.

---

## Resultado general

Los tres escenarios finales fueron validados correctamente:

- monto igual a 10.000.000 permitido;
- QR inválido rechazado;
- banco desconocido rechazado.

Con estas pruebas se completan las validaciones finales requeridas para la Tarea 2.
