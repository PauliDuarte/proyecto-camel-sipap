# Evidencia Tarea 2 - Idempotencia y duplicados

## Escenario

Se envió dos veces la misma transferencia utilizando el mismo identificador:

`TX-T2-DUP-001`

La transferencia era válida, con fecha actual, banco ITAU y monto `150000`.

## Primer envío

El primer POST fue aceptado con HTTP `202` y estado:

`ACEPTADA_PARA_PROCESAMIENTO`

La transferencia llegó al banco simulado y terminó con estado:

`PROCESADA`

## Segundo envío

Se volvió a enviar exactamente la misma transferencia con el mismo `id_transaccion`.

La API volvió a responder HTTP `202`, ya que el procesamiento es asíncrono.

El consumidor aplicó el patrón Idempotent Receiver y evitó procesar nuevamente la transferencia.

## Verificación en WireMock

Se consultó el journal de WireMock filtrando por:

`X-Transaction-Id: TX-T2-DUP-001`

El resultado fue:

`count = 1`

Esto demuestra que, aunque el mismo mensaje REST fue enviado dos veces, el banco simulado recibió una sola solicitud.

## Resultado

Se verificó correctamente el patrón:

`Idempotent Receiver`

utilizando `id_transaccion` como identificador único de la transferencia.
