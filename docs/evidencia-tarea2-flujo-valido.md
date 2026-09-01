# Evidencia Tarea 2 - Flujo válido ITAU

## Escenario

Transferencia válida recibida por REST, procesada mediante Apache Camel,
publicada en Apache ActiveMQ Artemis y enviada al banco simulado mediante WireMock.

### Datos utilizados

- ID transacción: `TX-T2-ITAU-20260901-001`
- Fecha: `2026-09-01`
- Banco: ITAU
- Código entidad: `0015`
- Cuenta: `100001`
- Monto: `150000`
- CRC: `A1B2`

## Respuesta REST

La API respondió:

- HTTP `202`
- Estado: `ACEPTADA_PARA_PROCESAMIENTO`
- Mensaje: `Transferencia publicada para procesamiento`

## Correlación

Durante el procesamiento se conservó el mismo identificador:

- `idTransaccion`: `TX-T2-ITAU-20260901-001`
- `JMSCorrelationID`: `TX-T2-ITAU-20260901-001`
- `X-Transaction-Id`: `TX-T2-ITAU-20260901-001`

Esto permite correlacionar la solicitud REST, el mensaje JMS y la petición HTTP
enviada al banco simulado.

## Artemis

Las rutas JMS se iniciaron correctamente:

- `sipap.entrada`
- `sipap.itau`
- `sipap.atlas`
- `sipap.familiar`

Para este escenario la transferencia fue direccionada al consumidor ITAU.

## WireMock

WireMock recibió la solicitud correspondiente al banco ITAU con:

- Código entidad: `0015`
- Cuenta: `100001`
- Monto: `150000`
- ID transacción: `TX-T2-ITAU-20260901-001`

La respuesta simulada produjo el estado final:

`PROCESADA`

con el mensaje:

`Transferencia procesada por el banco simulado`

## Resultado

Se verificó correctamente el flujo:

REST → Camel → validaciones → Artemis → cola ITAU → consumidor → WireMock → resultado correlacionado.
