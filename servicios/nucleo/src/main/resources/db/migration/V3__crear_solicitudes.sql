-- Solicitudes de disponibilidad (DN-09 a DN-15): el ciclo de vida, su historial, las claves de idempotencia de las
-- operaciones del turista y las notificaciones que se envían después de confirmar cada cambio.

CREATE TABLE solicitudes (
    id                       UUID          PRIMARY KEY,
    turista_id               UUID          NOT NULL,
    oferta_id                VARCHAR(50)   NOT NULL,
    -- Copia de la oferta al crear la solicitud (DC-35): no cambia si el prestador la edita o la retira.
    tipo_oferta              VARCHAR(20)   NOT NULL,
    nombre_oferta            VARCHAR(300)  NOT NULL,
    prestador_id             UUID          NOT NULL,
    zona_id                  VARCHAR(50)   NOT NULL,
    fecha_inicio             DATE          NOT NULL,
    -- Solo actividad y paquete (RF-30).
    hora_salida              TIME,
    personas                 INTEGER       NOT NULL,
    nota_turista             VARCHAR(500),
    -- El operador vinculado al prestador al crearla (RF-32).
    operador_id              UUID          NOT NULL,
    estado                   VARCHAR(20)   NOT NULL,
    -- Lo que registra el operador al atenderla (RF-37 a RF-39); vacío antes.
    precio_base              BIGINT,
    politica_cancelacion     VARCHAR(1000),
    nota_operador            VARCHAR(1000),
    -- El plazo vigente: el del operador o la ventana de confirmación y pago; vacío si está cerrada.
    vence_en                 TIMESTAMPTZ,
    -- Cuándo debe aplicarse el vencimiento: vence_en, o 30 min después con un intento de pago en curso (RF-55). Lo
    -- calcula el dominio; la consulta del vencimiento solo compara.
    limite_vencimiento       TIMESTAMPTZ,
    intento_pago_en_curso    BOOLEAN       NOT NULL,
    intentos_pago_fallidos   INTEGER       NOT NULL,
    creada_en                TIMESTAMPTZ   NOT NULL,
    actualizada_en           TIMESTAMPTZ   NOT NULL,
    -- Bloqueo optimista (ADR-05).
    revision                 BIGINT        NOT NULL,
    CONSTRAINT solicitudes_tipo_oferta CHECK (tipo_oferta IN ('ACTIVIDAD', 'PAQUETE', 'ALOJAMIENTO', 'RESTAURANTE')),
    CONSTRAINT solicitudes_estado CHECK (estado IN ('PENDIENTE', 'ATENDIDA', 'NO_DISPONIBLE', 'SIN_RESPUESTA',
                                                    'CONFIRMADA', 'DECLINADA', 'PAGO_FALLIDO', 'PAGADA', 'VENCIDA')),
    CONSTRAINT solicitudes_personas CHECK (personas >= 1),
    CONSTRAINT solicitudes_precio CHECK (precio_base IS NULL OR precio_base > 0),
    CONSTRAINT solicitudes_intentos CHECK (intentos_pago_fallidos BETWEEN 0 AND 3)
);

-- RF-31: un turista no puede tener dos solicitudes no terminales para la misma oferta, fecha y hora. Respalda a
-- ReglaNoDuplicado cuando dos envíos llegan a la vez, quizá a réplicas distintas. NULLS NOT DISTINCT: sin hora de
-- salida (alojamiento, restaurante) también cuentan como repetidas.
CREATE UNIQUE INDEX solicitudes_no_duplicada
    ON solicitudes (turista_id, oferta_id, fecha_inicio, hora_salida) NULLS NOT DISTINCT
    WHERE estado IN ('PENDIENTE', 'ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO');

-- La cola del operador por estado y antigüedad (RF-35).
CREATE INDEX solicitudes_cola ON solicitudes (operador_id, estado, creada_en);

-- Los vencimientos que toca aplicar (RF-41, RF-55): solo las abiertas.
CREATE INDEX solicitudes_por_vencer ON solicitudes (limite_vencimiento)
    WHERE estado IN ('PENDIENTE', 'ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO');

-- Las solicitudes del turista, de la más reciente a la más antigua (RF-33).
CREATE INDEX solicitudes_turista ON solicitudes (turista_id, creada_en DESC);

-- Historial de cambios de estado (RF-33, RF-40): solo se agregan filas.
CREATE TABLE transiciones_solicitud (
    id               UUID          PRIMARY KEY,
    solicitud_id     UUID          NOT NULL REFERENCES solicitudes (id),
    -- Posición en el historial, desde 0 (la creación).
    orden            INTEGER       NOT NULL,
    -- Vacío en la creación.
    estado_origen    VARCHAR(20),
    estado_destino   VARCHAR(20)   NOT NULL,
    ocurrida_en      TIMESTAMPTZ   NOT NULL,
    actor            VARCHAR(20)   NOT NULL,
    actor_id         UUID,
    motivo           VARCHAR(500),
    CONSTRAINT transiciones_solicitud_actor CHECK (actor IN ('TURISTA', 'OPERADOR', 'SISTEMA', 'PAGOS')),
    CONSTRAINT transiciones_solicitud_orden UNIQUE (solicitud_id, orden)
);

-- Claves de idempotencia de las operaciones del turista (ADR-04): enviar la solicitud y responder las condiciones. Una
-- por usuario y clave. Se reserva con INSERT ... ON CONFLICT en la transacción de la operación y la respuesta se guarda
-- en la misma: si la operación falla no queda nada, y el reintento con la misma clave vuelve a intentarlo (DN-12).
CREATE TABLE claves_idempotencia (
    usuario_id         UUID           NOT NULL,
    clave              UUID           NOT NULL,
    operacion          VARCHAR(100)   NOT NULL,
    -- SHA-256 de la operación, el recurso y el cuerpo, en hexadecimal: misma clave con otro cuerpo es un error.
    huella             VARCHAR(64)    NOT NULL,
    -- La respuesta original, tal cual se envió: se repite byte a byte. Vacías solo dentro de la transacción que la
    -- reservó, mientras la operación corre; ninguna otra transacción las ve así.
    codigo_respuesta   INTEGER,
    cuerpo_respuesta   TEXT,
    creada_en          TIMESTAMPTZ    NOT NULL,
    expira_en          TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (usuario_id, clave)
);

CREATE INDEX claves_idempotencia_expiradas ON claves_idempotencia (expira_en);

-- Notificaciones de los cambios de las solicitudes (DN-13): se guardan en la transacción del cambio y una tarea
-- programada envía las de WhatsApp, como la bandeja de salida (DN-06). Las de la plataforma son los avisos del
-- operador (RF-34).
CREATE TABLE notificaciones_solicitud (
    id                  UUID           PRIMARY KEY,
    solicitud_id        UUID           NOT NULL REFERENCES solicitudes (id),
    destinatario_tipo   VARCHAR(20)    NOT NULL,
    destinatario_id     UUID           NOT NULL,
    canal               VARCHAR(20)    NOT NULL,
    tipo                VARCHAR(40)    NOT NULL,
    -- Los parámetros de la plantilla; nunca números de teléfono.
    datos               JSONB          NOT NULL,
    -- PENDIENTE hasta enviarla; ENVIADA; FALLIDA si se agotan los intentos. Las de la plataforma nacen ENVIADA.
    estado              VARCHAR(20)    NOT NULL,
    intentos            INTEGER        NOT NULL,
    proximo_intento     TIMESTAMPTZ    NOT NULL,
    creada_en           TIMESTAMPTZ    NOT NULL,
    enviada_en          TIMESTAMPTZ,
    ultimo_error        VARCHAR(500),
    CONSTRAINT notificaciones_solicitud_destinatario CHECK (destinatario_tipo IN ('TURISTA', 'PRESTADOR', 'OPERADOR')),
    CONSTRAINT notificaciones_solicitud_canal CHECK (canal IN ('WHATSAPP', 'PLATAFORMA')),
    CONSTRAINT notificaciones_solicitud_estado CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA'))
);

-- Las de WhatsApp por enviar, en el orden en que se toman.
CREATE INDEX notificaciones_solicitud_pendientes ON notificaciones_solicitud (proximo_intento, creada_en)
    WHERE estado = 'PENDIENTE';

-- Los avisos de cada operador, del más reciente al más antiguo (RF-34).
CREATE INDEX notificaciones_solicitud_avisos ON notificaciones_solicitud (destinatario_id, creada_en DESC)
    WHERE canal = 'PLATAFORMA';
