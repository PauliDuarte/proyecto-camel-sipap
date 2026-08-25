# Evidencias de ejecución

Verificación realizada en la rama `rama_steven` el 25 de agosto de 2026. Los fragmentos incluidos son líneas breves obtenidas de la ejecución real; no son capturas ni resultados simulados.

## Pruebas automatizadas

Comando utilizado:

```bash
./mvnw test
```

Resultado real:

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

La suite verificó el arranque del contexto, parsing y longitudes TLV, reglas de validación, los tres bancos conocidos y el rechazo de un banco desconocido sin entregar el mensaje a consumidores bancarios. En `TransferenciaRouteTest`, los mocks esperaron un mensaje en rechazados y cero mensajes en ITAU, ATLAS y FAMILIAR.

## Ejecución de la aplicación

Comando utilizado:

```bash
./mvnw spring-boot:run
```

La aplicación se mantuvo activa durante la primera rotación completa de ambos productores y luego se detuvo de forma controlada.

| Escenario | ID | Productor | Banco o causa | Estado |
|---|---|---|---|---|
| ITAU válido | `TX000001` | Productor A | ITAU (`0015`) | `PROCESADA` |
| ATLAS válido | `TX000002` | Productor B | ATLAS (`0007`) | `PROCESADA` |
| FAMILIAR válido | `TX000003` | Productor A | FAMILIAR (`0020`) | `PROCESADA` |
| Banco desconocido | `TX000004` | Productor B | Código `9999` | `RECHAZADA` |
| Longitud TLV incorrecta | `TX000005` | Productor A | Longitud declarada del tag `59` | `RECHAZADA` |
| Monto en el límite | `TX000006` | Productor B | Monto `10000000` | `RECHAZADA` |
| CRC inválido | `TX000007` | Productor A | CRC `FFFF` | `RECHAZADA` |

## Fragmentos de logs

### 1. ITAU válido

```text
[PRODUCTOR A]
[ITAU] Modelo recibido: Transferencia[
idTransaccion=TX000001
codigoEntidad=0015
transactionAmount=150000
crc=A1B2
[ITAU] Resultado: ResultadoTransferencia[idTransaccion=TX000001, estado=PROCESADA, mensaje=Transferencia procesada exitosamente]
```

Evidencia que el Productor A generó el caso, que ITAU recibió un objeto `Transferencia` y que el resultado conservó el ID con estado procesado.

### 2. ATLAS válido

```text
[PRODUCTOR B]
[ATLAS] Modelo recibido: Transferencia[
idTransaccion=TX000002
codigoEntidad=0007
transactionAmount=250000
crc=A1B2
[ATLAS] Resultado: ResultadoTransferencia[idTransaccion=TX000002, estado=PROCESADA, mensaje=Transferencia procesada exitosamente]
```

Evidencia el enrutamiento del objeto canónico generado por el Productor B hacia ATLAS.

### 3. FAMILIAR válido

```text
[PRODUCTOR A]
[FAMILIAR] Modelo recibido: Transferencia[
idTransaccion=TX000003
codigoEntidad=0020
transactionAmount=350000
crc=A1B2
[FAMILIAR] Resultado: ResultadoTransferencia[idTransaccion=TX000003, estado=PROCESADA, mensaje=Transferencia procesada exitosamente]
```

Evidencia el enrutamiento del objeto canónico generado por el Productor A hacia FAMILIAR.

### 4. Banco desconocido

```text
[PRODUCTOR B]
[PARSEO] TX000004 - Transferencia[
idTransaccion=TX000004
codigoEntidad=9999
transactionAmount=300000
crc=A1B2
[RECHAZADA] ResultadoTransferencia[idTransaccion=TX000004, estado=RECHAZADA, mensaje=Código de entidad desconocido: 9999]
```

Evidencia que el parser creó el modelo, las validaciones generales terminaron y `choice()/otherwise` rechazó el banco. La prueba de ruta complementa este log comprobando cero entregas a los tres bancos.

### 5. Longitud TLV incorrecta

```text
[PRODUCTOR A] 0002010102125909JUAN
[ENTRADA SIPAP] TX000005 - 0002010102125909JUAN
[RECHAZADA] ResultadoTransferencia[idTransaccion=TX000005, estado=RECHAZADA, mensaje=Longitud declarada no coincide con el contenido para el tag 59]
```

Evidencia una verificación real entre la longitud `09` declarada y el contenido insuficiente. No aparece una entrega bancaria para `TX000005`.

### 6. Monto mayor o igual al límite

```text
[PRODUCTOR B]
[PARSEO] TX000006 - Transferencia[
idTransaccion=TX000006
codigoEntidad=0007
transactionAmount=10000000
crc=A1B2
[RECHAZADA] ResultadoTransferencia[idTransaccion=TX000006, estado=RECHAZADA, mensaje=transactionAmount debe ser menor a 10000000]
```

Evidencia que el valor exactamente igual a `10000000` es rechazado. No aparece una entrega a ATLAS para `TX000006`.

### 7. CRC inválido

```text
[PRODUCTOR A]
[PARSEO] TX000007 - Transferencia[
idTransaccion=TX000007
codigoEntidad=0015
transactionAmount=450000
crc=FFFF
[RECHAZADA] ResultadoTransferencia[idTransaccion=TX000007, estado=RECHAZADA, mensaje=CRC inválido: se esperaba A1B2]
```

Evidencia que el CRC diferente de `A1B2` es rechazado y no se entrega a ITAU.

## Observaciones de entrega

- Los tres consumidores válidos registraron `Modelo recibido: Transferencia[...]`, no la cadena TLV original.
- Los casos `TX000004` a `TX000007` terminaron en rechazados y no mostraron líneas de consumidores bancarios con esos IDs.
- La prueba automatizada de banco desconocido confirma explícitamente una entrega a rechazados y cero entregas a ITAU, ATLAS y FAMILIAR.
- `TX000004`, al superar parsing y validación, también produjo `[AUDITORIA]` antes de ser rechazado por banco desconocido.
- Los errores `TX000005`, `TX000006` y `TX000007` ocurrieron antes de `wireTap`, por lo que no fueron enviados a auditoría.
