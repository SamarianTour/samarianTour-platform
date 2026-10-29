-- Datos maestros de los prestadores (módulo 7, RF-62, RF-63, RF-68 a RF-71, RF-73, DN-33). El perfil público y las
-- ofertas viven en el catálogo. El identificador es el de la cuenta del prestador (usuarios, V8).
CREATE TABLE prestadores (
    id                       UUID          PRIMARY KEY REFERENCES usuarios (id),
    numero_rnt               VARCHAR(10)   NOT NULL,
    tipo                     VARCHAR(30)   NOT NULL,
    -- Vacío para el guía independiente (RF-68).
    numero_empleados         INTEGER,
    -- Solo el guía independiente (RF-69): listas separadas por comas, como CAMINATA,KAYAK y es,en.
    modalidades              VARCHAR(100),
    idiomas                  VARCHAR(100),
    -- Lo que dijo el RNT la última vez que respondió (RF-63, RF-70).
    razon_social             VARCHAR(300),
    municipio                VARCHAR(100),
    categoria_rnt            VARCHAR(200),
    persona_natural_rnt      BOOLEAN       NOT NULL DEFAULT FALSE,
    estado_rnt               VARCHAR(30)   NOT NULL,
    -- Si alguna vez apareció en el RNT: al desaparecer queda INACTIVO y no NO_ENCONTRADO (DN-33).
    visto_en_rnt             BOOLEAN       NOT NULL DEFAULT FALSE,
    ultima_sincronizacion    TIMESTAMPTZ,
    -- Cuándo lo tomó la última sincronización (RF-76, DN-36), responda o no el RNT: reparte el trabajo entre las
    -- réplicas con FOR UPDATE SKIP LOCKED. No es del dominio.
    sincronizacion_tomada_en TIMESTAMPTZ,
    -- Cifrado con AES-GCM (RNF-08, RF-71); nunca es público.
    whatsapp_cifrado         TEXT,
    whatsapp_verificado      BOOLEAN       NOT NULL DEFAULT FALSE,
    -- El código pendiente para verificarlo (DN-33): solo su hash, cuándo se emitió y los intentos fallidos.
    whatsapp_codigo_hash     VARCHAR(100),
    whatsapp_codigo_emitido  TIMESTAMPTZ,
    whatsapp_codigo_intentos INTEGER       NOT NULL DEFAULT 0,
    -- El operador que atiende sus solicitudes, asignado de forma estática (RF-32, RF-81).
    operador_id              UUID          REFERENCES usuarios (id),
    -- La réplica del catálogo (DC-02): lo último que se le avisó y su versión, que solo crece.
    habilitado_en_catalogo   BOOLEAN       NOT NULL DEFAULT FALSE,
    version_habilitacion     BIGINT        NOT NULL DEFAULT 0,
    fecha_registro           TIMESTAMPTZ   NOT NULL,
    revision                 BIGINT        NOT NULL,
    CONSTRAINT prestadores_tipo CHECK (tipo IN ('ALOJAMIENTO', 'RESTAURANTE', 'AGENCIA_OPERADORA',
                                                'GUIA_INDEPENDIENTE')),
    CONSTRAINT prestadores_estado_rnt CHECK (estado_rnt IN ('ACTIVO', 'INACTIVO', 'NO_ENCONTRADO',
                                                            'VERIFICACION_PENDIENTE')),
    -- Un RNT es de un solo prestador [SUPUESTO] (DN-33).
    CONSTRAINT prestadores_rnt_unico UNIQUE (numero_rnt)
);

-- Las cuentas de cobro con su historial (RF-74, RF-75, DN-34): cada cambio cierra la anterior. El número y el titular
-- van cifrados (RNF-08); solo la dispersión los lee completos.
CREATE TABLE cuentas_cobro (
    id               UUID          PRIMARY KEY,
    prestador_id     UUID          NOT NULL REFERENCES prestadores (id),
    tipo             VARCHAR(20)   NOT NULL,
    entidad          VARCHAR(100)  NOT NULL,
    numero_cifrado   TEXT          NOT NULL,
    titular_cifrado  TEXT          NOT NULL,
    vigente_desde    TIMESTAMPTZ   NOT NULL,
    vigente_hasta    TIMESTAMPTZ,
    CONSTRAINT cuentas_cobro_tipo CHECK (tipo IN ('AHORROS', 'CORRIENTE', 'NEQUI', 'DAVIPLATA'))
);

-- Una sola vigente por prestador, aunque dos réplicas registren a la vez.
CREATE UNIQUE INDEX cuentas_cobro_una_vigente ON cuentas_cobro (prestador_id) WHERE vigente_hasta IS NULL;

-- La auditoría de los cambios delicados del prestador (RF-74, RNF-09). Nunca guarda un número completo.
CREATE TABLE auditoria_prestadores (
    id            UUID          PRIMARY KEY,
    prestador_id  UUID          NOT NULL REFERENCES prestadores (id),
    accion        VARCHAR(50)   NOT NULL,
    detalle       VARCHAR(300)  NOT NULL,
    ocurrido_en   TIMESTAMPTZ   NOT NULL
);

CREATE INDEX auditoria_prestadores_por_prestador ON auditoria_prestadores (prestador_id, ocurrido_en);
