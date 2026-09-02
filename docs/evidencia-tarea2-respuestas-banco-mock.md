# Evidencia Tarea 2 - Respuestas del banco simulado

## Escenario 1 - Rechazo del banco

Se envió una transferencia válida utilizando la cuenta reservada:

`888888`

ID:

`TX-T2-MOCK-422-001`

La API respondió inicialmente HTTP `202` con estado:

`ACEPTADA_PARA_PROCESAMIENTO`

WireMock recibió la solicitud para ITAU manteniendo el identificador de correlación.

La respuesta del banco simulado fue:

- Estado: `RECHAZADA`
- Mensaje: `Transferencia rechazada por el banco simulado`

Esto demuestra que el mediador procesa correctamente una respuesta de rechazo del banco.

## Escenario 2 - Error técnico del banco

Se envió otra transferencia válida utilizando la cuenta reservada:

`999999`

ID:

`TX-T2-MOCK-500-001`

La API respondió inicialmente HTTP `202` con estado:

`ACEPTADA_PARA_PROCESAMIENTO`

WireMock recibió la solicitud manteniendo el mismo identificador de transacción.

La respuesta del banco simulado fue:

- Estado: `ERROR_BANCO`
- Mensaje: `Error técnico del banco simulado`

Esto demuestra que el mediador puede procesar y registrar un error técnico devuelto por el banco simulado.

## Correlación

En ambos casos se mantuvo el identificador de la transferencia mediante:

- `idTransaccion`
- `JMSCorrelationID`
- `X-Transaction-Id`

Por lo tanto, se puede correlacionar la solicitud REST, el mensaje JMS y la comunicación con el banco simulado.
