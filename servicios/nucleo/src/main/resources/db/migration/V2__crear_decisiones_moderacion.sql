-- Historial de las decisiones de moderación (DN-08): quién decidió qué, cuándo y por qué. Una por versión de oferta y
-- una por opinión; es lo que permite reconocer una decisión repetida.
CREATE TABLE decisiones_moderacion (
    id                UUID          PRIMARY KEY,
    tipo              VARCHAR(10)   NOT NULL,
    -- Identificador de la oferta o de la opinión en el catálogo.
    moderado_id       VARCHAR(50)   NOT NULL,
    -- Solo para las ofertas; una opinión no tiene versiones.
    numero_version    INTEGER,
    decision          VARCHAR(10)   NOT NULL,
    motivo            VARCHAR(1000),
    administrador_id  UUID          NOT NULL,
    decidida_en       TIMESTAMPTZ   NOT NULL,
    id_aviso          VARCHAR(200)  NOT NULL,
    CONSTRAINT decisiones_moderacion_tipo CHECK (tipo IN ('OFERTA', 'OPINION')),
    CONSTRAINT decisiones_moderacion_decision CHECK (decision IN ('APROBAR', 'RECHAZAR')),
    CONSTRAINT decisiones_moderacion_version CHECK ((tipo = 'OFERTA') = (numero_version IS NOT NULL)),
    -- Una sola decisión por versión de oferta y por opinión, aunque dos réplicas guarden a la vez. NULLS NOT DISTINCT
    -- hace que las opiniones, sin versión, también cuenten como repetidas.
    CONSTRAINT decisiones_moderacion_unica UNIQUE NULLS NOT DISTINCT (tipo, moderado_id, numero_version)
);
