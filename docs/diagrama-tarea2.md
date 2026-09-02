# Diagrama de arquitectura de la Tarea 2

El diagrama representa el flujo implementado desde la entrada REST hasta el resultado del banco simulado. Las líneas punteadas muestran la conservación de `id_transaccion` como Correlation Identifier entre REST, JMS y WireMock.

```mermaid
flowchart TD
    CLIENTE["Cliente / Producer REST"] --> API["POST /api/transferencias"]

    subgraph CAMEL_PRE["Apache Camel - ingreso y validación"]
        API --> DIRECT_API["direct:api-transferencias"]
        DIRECT_API --> PARSER["Parsing TLV<br/>TlvParserProcessor"]
        PARSER --> VALIDAR["Validaciones de QR<br/>ValidacionProcessor"]
        VALIDAR --> MONTO["Validación de monto<br/>MontoProcessor"]
        MONTO --> BANCO["Validación de banco conocido<br/>TransferenciaRoute"]
        BANCO --> VALIDO{"¿Validaciones correctas?"}
        VALIDO -->|No| RECHAZO_PRE["HTTP 400<br/>RECHAZADA"]
        VALIDO -->|Sí| ENTRADA["Publicar en sipap.entrada"]
    end

    RECHAZO_PRE --> SIN_ARTEMIS["No se publica en Artemis"]
    ENTRADA --> ACEPTADA["HTTP 202<br/>ACEPTADA_PARA_PROCESAMIENTO"]

    subgraph ARTEMIS["Apache ActiveMQ Artemis"]
        COLA_ENTRADA["sipap.entrada"] --> DISTRIBUIDOR{"Distribuidor por banco<br/>choice() por codigoEntidad"}
        DISTRIBUIDOR -->|0015 ITAU| COLA_ITAU["sipap.itau"]
        DISTRIBUIDOR -->|0007 ATLAS| COLA_ATLAS["sipap.atlas"]
        DISTRIBUIDOR -->|0020 FAMILIAR| COLA_FAMILIAR["sipap.familiar"]
    end

    ENTRADA --> COLA_ENTRADA

    COLA_ITAU --> CONSUMIDOR_ITAU["Consumidor ITAU"]
    COLA_ATLAS --> CONSUMIDOR_ATLAS["Consumidor ATLAS"]
    COLA_FAMILIAR --> CONSUMIDOR_FAMILIAR["Consumidor FAMILIAR"]

    CONSUMIDOR_ITAU --> FECHA["Validación de fecha<br/>America/Asuncion"]
    CONSUMIDOR_ATLAS --> FECHA
    CONSUMIDOR_FAMILIAR --> FECHA

    FECHA --> FECHA_OK{"¿Fecha actual?"}
    FECHA_OK -->|No| RECHAZADA_FECHA["RECHAZADA_FECHA"]
    RECHAZADA_FECHA --> SIN_MOCK_FECHA["No llamar al banco mock"]
    FECHA_OK -->|Sí| IDEMPOTENCIA["Idempotent Receiver<br/>clave: id_transaccion"]
    IDEMPOTENCIA --> DUPLICADA_Q{"¿Duplicada?"}
    DUPLICADA_Q -->|Sí| DUPLICADA["DUPLICADA"]
    DUPLICADA --> SIN_MOCK_DUP["No llamar nuevamente al banco mock"]
    DUPLICADA_Q -->|No| REQUEST_REPLY["Request-Reply<br/>direct:invocar-banco"]
    REQUEST_REPLY --> WIREMOCK["WireMock / banco simulado<br/>POST /bancos/{banco}/transferencias"]
    WIREMOCK --> RESPUESTA{"Respuesta HTTP del banco"}
    RESPUESTA -->|2xx| PROCESADA["PROCESADA"]
    RESPUESTA -->|422| RECHAZADA_BANCO["RECHAZADA"]
    RESPUESTA -->|otro estado| ERROR_BANCO["ERROR_BANCO"]

    CORR_REST["REST: id_transaccion"] -. "Correlation Identifier" .-> CORR_JMS["JMS: idTransaccion / JMSCorrelationID"]
    CORR_JMS -. "mismo identificador" .-> CORR_HTTP["WireMock: X-Transaction-Id"]
```

La respuesta HTTP `202` confirma que la transferencia válida fue publicada para procesamiento; los estados posteriores se producen de forma asíncrona. Los casos `RECHAZADA_FECHA` y `DUPLICADA` terminan antes de `direct:invocar-banco`, por lo que no generan una llamada a WireMock.
