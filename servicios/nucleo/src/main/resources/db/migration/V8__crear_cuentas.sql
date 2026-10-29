-- Cuentas, consentimientos, códigos de un solo uso y sesiones (módulo 1, RF-01 a RF-13, DN-18, DN-19).

-- Cada versión publicada de la política de tratamiento de datos (RF-10, RF-11). La vigente es la más reciente.
CREATE TABLE politicas_datos (
    version            VARCHAR(20)   PRIMARY KEY,
    fecha_publicacion  DATE          NOT NULL,
    url_texto          VARCHAR(300)  NOT NULL
);

INSERT INTO politicas_datos (version, fecha_publicacion, url_texto)
VALUES ('v1', DATE '2026-10-28', '/api/nucleo/politica-datos/v1');

-- Una fila por cuenta, con su rol. El nombre y el WhatsApp van cifrados con AES-GCM (RNF-08, DN-18); el correo, en
-- claro y en minúsculas, porque es con lo que se inicia sesión.
CREATE TABLE usuarios (
    id                   UUID          PRIMARY KEY,
    rol                  VARCHAR(30)   NOT NULL,
    nombre_cifrado       TEXT          NOT NULL,
    correo               VARCHAR(254)  NOT NULL,
    -- BCrypt; vacío en una cuenta suprimida o en una interna que todavía no tiene contraseña (RF-81).
    hash_contrasena      VARCHAR(100),
    estado               VARCHAR(20)   NOT NULL,
    intentos_fallidos    INTEGER       NOT NULL DEFAULT 0,
    bloqueada_hasta      TIMESTAMPTZ,
    fecha_registro       TIMESTAMPTZ   NOT NULL,
    -- Solo el turista (RF-03).
    whatsapp_cifrado     TEXT,
    whatsapp_verificado  BOOLEAN       NOT NULL DEFAULT FALSE,
    revision             BIGINT        NOT NULL,
    CONSTRAINT usuarios_rol CHECK (rol IN ('TURISTA', 'PRESTADOR', 'ADMINISTRADOR', 'OPERADOR_DISPONIBILIDAD',
                                            'GESTOR_DESTINO')),
    CONSTRAINT usuarios_estado CHECK (estado IN ('ACTIVA', 'DESACTIVADA', 'SUPRIMIDA')),
    CONSTRAINT usuarios_whatsapp_solo_turista CHECK (rol = 'TURISTA' OR whatsapp_cifrado IS NULL),
    -- Un correo, una cuenta (RF-02), aunque dos réplicas registren a la vez.
    CONSTRAINT usuarios_correo_unico UNIQUE (correo)
);

-- Constancia de cada aceptación de la política: qué versión y cuándo (RF-10, RNF-09). Solo se agregan filas.
CREATE TABLE aceptaciones_politica (
    usuario_id        UUID          NOT NULL REFERENCES usuarios (id),
    version           VARCHAR(20)   NOT NULL REFERENCES politicas_datos (version),
    fecha_aceptacion  TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (usuario_id, version)
);

-- La petición del titular de eliminar sus datos (RF-13): queda aunque los datos ya no estén.
CREATE TABLE solicitudes_supresion (
    id               UUID          PRIMARY KEY,
    usuario_id       UUID          NOT NULL UNIQUE REFERENCES usuarios (id),
    fecha_solicitud  TIMESTAMPTZ   NOT NULL,
    estado           VARCHAR(20)   NOT NULL,
    fecha_ejecucion  TIMESTAMPTZ,
    CONSTRAINT solicitudes_supresion_estado CHECK (estado IN ('SOLICITADA', 'EJECUTADA')),
    CONSTRAINT solicitudes_supresion_ejecucion CHECK ((estado = 'EJECUTADA') = (fecha_ejecucion IS NOT NULL))
);

-- Códigos de un solo uso: verificar el WhatsApp, restablecer o establecer la contraseña (RF-03, RF-07, RF-08, RF-81).
-- Solo el hash del código.
CREATE TABLE codigos_un_uso (
    id                 UUID          PRIMARY KEY,
    usuario_id         UUID          NOT NULL REFERENCES usuarios (id),
    proposito          VARCHAR(30)   NOT NULL,
    hash_codigo        VARCHAR(100)  NOT NULL,
    emitido_en         TIMESTAMPTZ   NOT NULL,
    expira_en          TIMESTAMPTZ   NOT NULL,
    usado_en           TIMESTAMPTZ,
    intentos_fallidos  INTEGER       NOT NULL DEFAULT 0,
    revision           BIGINT        NOT NULL,
    CONSTRAINT codigos_un_uso_proposito CHECK (proposito IN ('VERIFICACION_WHATSAPP', 'RESTABLECER_CONTRASENA',
                                                             'ESTABLECER_CONTRASENA'))
);

CREATE INDEX codigos_un_uso_ultimo ON codigos_un_uso (usuario_id, proposito, emitido_en DESC);

-- Sesiones con el hash del token de renovación vigente (RF-04, RF-06, DN-19).
CREATE TABLE sesiones (
    id               UUID          PRIMARY KEY,
    usuario_id       UUID          NOT NULL REFERENCES usuarios (id),
    hash_renovacion  VARCHAR(100)  NOT NULL,
    creada_en        TIMESTAMPTZ   NOT NULL,
    expira_en        TIMESTAMPTZ   NOT NULL,
    revocada_en      TIMESTAMPTZ,
    revision         BIGINT        NOT NULL
);

CREATE INDEX sesiones_abiertas ON sesiones (usuario_id) WHERE revocada_en IS NULL;
