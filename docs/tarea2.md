# Tarea 2 - API REST, Artemis y banco simulado

La entrada principal es `POST /api/transferencias`. El identificador recibido se conserva en el modelo, los headers Camel/JMS, `JMSCorrelationID`, el request al banco y el resultado.

## Infraestructura local

```bash
docker compose up -d
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api/transferencias`
- Artemis: `tcp://localhost:61616`
- Consola Artemis: `http://localhost:8161`, usuario y contraseña `artemis`
- WireMock: `http://localhost:8081`

Las colas punto a punto son `sipap.entrada`, `sipap.itau`, `sipap.atlas` y `sipap.familiar`.

## Ejemplo de entrada

```json
{
  "id_transaccion": "TX000001",
  "fecha_transaccion": "2026-08-25",
  "qr": "000201010212...",
  "monto": "150000"
}
```

Para QR estático se usa el monto externo. Para QR dinámico, el monto del QR es autoritativo y debe coincidir con el externo. Los montos de hasta `10000000`, inclusive, se aceptan; un monto mayor se rechaza antes del broker con `El monto supera máximo permitido`.

## Banco simulado

WireMock expone `POST /bancos/{banco}/transferencias`:

- cuenta normal: HTTP 200 y `PROCESADA`;
- cuenta reservada `888888`: HTTP 422 y `RECHAZADA`;
- cuenta reservada `999999`: HTTP 500 y `ERROR_BANCO`.

La fecha se compara en la zona `America/Asuncion`. Una fecha diferente produce `RECHAZADA_FECHA` sin invocar WireMock.

El Idempotent Receiver usa un `MemoryIdempotentRepository` compartido. Un segundo mensaje con el mismo identificador produce `DUPLICADA` y no vuelve a invocar el banco. El repositorio vive solamente en memoria, se pierde al reiniciar y no coordina varias instancias.

## Patrones EIP nuevos

- Message Channel: las cuatro colas Artemis.
- Idempotent Receiver: `idempotentConsumer` por `idTransaccion`.
- Correlation Identifier: el mismo identificador acompaña todo el flujo.
- Request-Reply: cada consumidor realiza un POST síncrono al banco simulado.
