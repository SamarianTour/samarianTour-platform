-- Pagos (DN-18 a DN-27): los intentos en la pasarela, los eventos de su webhook, los pagos tardíos, la comisión con su
-- historial, las dispersiones al prestador y las claves de idempotencia de las operaciones de pago del turista.
-- Ninguna tabla guarda datos de tarjeta ni de cuenta del turista (RF-47, RNF-08).

-- Cada vez que el turista abre el pago de una solicitud (RF-46 a RF-49, RF-52, RF-58).
CREATE TABLE intentos_pago (
    id                 UUID           PRIMARY KEY,
    solicitud_id       UUID           NOT NULL,
    turista_id         UUID           NOT NULL,
    -- El orden del intento en la solicitud, desde 1. Los abandonados también llevan número (DN-20).
    numero             INTEGER        NOT NULL,
    -- La que la pasarela devuelve en sus eventos; Wompi la exige única por transacción.
    referencia         VARCHAR(255)   NOT NULL,
    -- El precio base exacto en centavos de peso, sin cargos adicionales (RF-49).
    monto_centavos     BIGINT         NOT NULL,
    estado             VARCHAR(20)    NOT NULL,
    -- El id de la transacción en la pasarela, desde su primer evento (RF-52).
    transaccion_id     VARCHAR(100),
    creado_en          TIMESTAMPTZ    NOT NULL,
    resuelto_en        TIMESTAMPTZ,
    -- Bloqueo optimista entre el procesamiento de eventos y el abandono (ADR-05).
    revision           BIGINT         NOT NULL,
    CONSTRAINT intentos_pago_referencia UNIQUE (referencia),
    CONSTRAINT intentos_pago_numero CHECK (numero >= 1),
    CONSTRAINT intentos_pago_monto CHECK (monto_centavos > 0),
    CONSTRAINT intentos_pago_estado CHECK (estado IN ('EN_CURSO', 'APROBADO', 'RECHAZADO', 'ERROR', 'ABANDONADO'))
);

-- Un solo intento en curso por solicitud: dos peticiones a la vez, quizá en réplicas distintas, no abren dos.
CREATE UNIQUE INDEX intentos_pago_uno_en_curso ON intentos_pago (solicitud_id) WHERE estado = 'EN_CURSO';

-- Los de una solicitud en orden, para la reconsulta del turista (ADR-04).
CREATE INDEX intentos_pago_solicitud ON intentos_pago (solicitud_id, numero);

-- Los en curso por antigüedad, para abandonarlos a los 30 min con SKIP LOCKED (DN-20).
CREATE INDEX intentos_pago_por_abandonar ON intentos_pago (creado_en) WHERE estado = 'EN_CURSO';

-- Lo que llegó por el webhook, ya verificado (RF-50, DN-21). Se guarda al recibirlo y se procesa después.
CREATE TABLE eventos_pasarela (
    id                 UUID           PRIMARY KEY,
    -- El id de la transacción y su estado [SUPUESTO]: Wompi no manda un id de evento y reintenta el envío.
    clave_unica        VARCHAR(150)   NOT NULL,
    transaccion_id     VARCHAR(100)   NOT NULL,
    referencia         VARCHAR(255)   NOT NULL,
    estado             VARCHAR(20)    NOT NULL,
    monto_centavos     BIGINT         NOT NULL,
    -- El cuerpo tal como llegó, para la trazabilidad (RF-52); TEXT y no JSONB para no reordenarlo.
    cuerpo             TEXT           NOT NULL,
    recibido_en        TIMESTAMPTZ    NOT NULL,
    procesado_en       TIMESTAMPTZ,
    -- APLICADO, SIN_CAMBIOS, PAGO_TARDIO, SIN_INTENTO, MONTO_DISTINTO o el código del error.
    resultado          VARCHAR(50),
    CONSTRAINT eventos_pasarela_clave UNIQUE (clave_unica),
    CONSTRAINT eventos_pasarela_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'ANULADA', 'ERROR'))
);

-- Los que faltan por procesar, del más antiguo al más reciente, para tomarlos con SKIP LOCKED.
CREATE INDEX eventos_pasarela_sin_procesar ON eventos_pasarela (recibido_en) WHERE procesado_en IS NULL;

-- Aprobados que llegaron cuando la solicitud ya estaba vencida o cerrada (RF-50, DN-22): se reembolsan a mano.
CREATE TABLE pagos_tardios (
    id                 UUID           PRIMARY KEY,
    solicitud_id       UUID           NOT NULL,
    intento_id         UUID           NOT NULL REFERENCES intentos_pago (id),
    referencia         VARCHAR(255)   NOT NULL,
    transaccion_id     VARCHAR(100)   NOT NULL,
    monto_centavos     BIGINT         NOT NULL,
    registrado_en      TIMESTAMPTZ    NOT NULL,
    -- Cuándo se avisó al administrador por WhatsApp; vacío mientras no se pueda.
    aviso_enviado_en   TIMESTAMPTZ,
    -- Uno por transacción: un mismo intento puede tener más de una aprobada que no paga nada.
    CONSTRAINT pagos_tardios_transaccion UNIQUE (transaccion_id)
);

CREATE INDEX pagos_tardios_sin_aviso ON pagos_tardios (registrado_en) WHERE aviso_enviado_en IS NULL;

-- El porcentaje de comisión con su historial (RF-82, RF-83, DN-23): cada cambio agrega una fila.
CREATE TABLE configuracion_comision (
    id                 UUID           PRIMARY KEY,
    porcentaje         NUMERIC(5, 2)  NOT NULL,
    vigente_desde      TIMESTAMPTZ    NOT NULL,
    -- Quién lo cambió; vacío en el inicial.
    administrador_id   UUID,
    CONSTRAINT configuracion_comision_porcentaje CHECK (porcentaje >= 0 AND porcentaje < 100)
);

-- El inicial: 4 % sobre el precio base, ratificado por el equipo (SRS, anexo 5.4).
INSERT INTO configuracion_comision (id, porcentaje, vigente_desde, administrador_id)
VALUES ('00000000-0000-0000-0000-000000000004', 4.00, '2026-01-01T00:00:00Z', NULL);

-- Lo que se transfiere al prestador de cada pago confirmado (RF-53, RF-54, DN-24). La liquidación no cambia después
-- de crearse (RF-83).
CREATE TABLE dispersiones (
    id                    UUID           PRIMARY KEY,
    solicitud_id          UUID           NOT NULL,
    prestador_id          UUID           NOT NULL,
    monto_bruto           BIGINT         NOT NULL,
    porcentaje_comision   NUMERIC(5, 2)  NOT NULL,
    comision              BIGINT         NOT NULL,
    costo_transaccional   BIGINT         NOT NULL,
    monto_neto            BIGINT         NOT NULL,
    estado                VARCHAR(20)    NOT NULL,
    intentos              INTEGER        NOT NULL,
    ultima_causa          VARCHAR(500),
    -- Solo los últimos 4 dígitos de la cuenta a la que se envió (RF-75).
    cuenta_destino        VARCHAR(20),
    id_pasarela           VARCHAR(100),
    creada_en             TIMESTAMPTZ    NOT NULL,
    proximo_intento       TIMESTAMPTZ,
    ejecutada_en          TIMESTAMPTZ,
    aviso_enviado_en      TIMESTAMPTZ,
    CONSTRAINT dispersiones_solicitud UNIQUE (solicitud_id),
    CONSTRAINT dispersiones_estado CHECK (estado IN ('PENDIENTE', 'EXITOSA', 'FALLIDA')),
    CONSTRAINT dispersiones_montos CHECK (monto_bruto > 0 AND comision >= 0 AND costo_transaccional >= 0
                                          AND monto_neto >= 0)
);

-- Las pendientes cuyo próximo intento ya toca, para tomarlas con SKIP LOCKED.
CREATE INDEX dispersiones_pendientes ON dispersiones (proximo_intento) WHERE estado = 'PENDIENTE';

-- Las fallidas que todavía no se avisaron al prestador.
CREATE INDEX dispersiones_fallidas_sin_aviso ON dispersiones (creada_en)
    WHERE estado = 'FALLIDA' AND aviso_enviado_en IS NULL;

-- Claves de idempotencia de pagos (ADR-04, DN-19), como las de las solicitudes (DN-12): cada módulo tiene las suyas.
CREATE TABLE claves_idempotencia_pagos (
    usuario_id         UUID           NOT NULL,
    clave              UUID           NOT NULL,
    operacion          VARCHAR(100)   NOT NULL,
    huella             VARCHAR(64)    NOT NULL,
    codigo_respuesta   INTEGER,
    cuerpo_respuesta   TEXT,
    creada_en          TIMESTAMPTZ    NOT NULL,
    expira_en          TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (usuario_id, clave)
);

CREATE INDEX claves_idempotencia_pagos_expiradas ON claves_idempotencia_pagos (expira_en);
