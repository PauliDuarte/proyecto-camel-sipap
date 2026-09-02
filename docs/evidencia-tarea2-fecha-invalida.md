# Evidencia Tarea 2 - Fecha inválida

## Escenario

Se envió una transferencia con una fecha distinta a la fecha actual de ejecución.

### Datos

- ID: `TX-T2-FECHA-001`
- Fecha enviada: `2026-08-31`
- Fecha de ejecución: `2026-09-01`
- Banco: ITAU
- Monto: `150000`

## Respuesta REST

La API respondió HTTP `202` con estado:

`ACEPTADA_PARA_PROCESAMIENTO`

Esto indica que la transferencia ingresó al flujo asíncrono.

## Validación de fecha

Durante el consumo del mensaje se obtuvo:

`fechaValida: false`

El consumidor detectó que la fecha de la transferencia no correspondía con la fecha actual.

## Verificación de WireMock

Se buscaron solicitudes con el identificador:

`TX-T2-FECHA-001`

WireMock no registró ninguna solicitud asociada a ese identificador.

Esto demuestra que una transferencia con fecha inválida no llega al banco simulado.

## Resultado esperado

`RECHAZADA_FECHA`

## Resultado

Se verificó correctamente el flujo:

REST → Artemis → consumidor ITAU → validación de fecha → rechazo sin invocar WireMock.
