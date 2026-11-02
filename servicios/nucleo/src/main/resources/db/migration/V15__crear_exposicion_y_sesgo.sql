-- El registro de exposición de la recomendación (RET-02, DN-42): una fila por oferta mostrada en una consulta, sin el
-- texto ni ningún dato del turista. Se guardan 90 días [SUPUESTO]; el índice de sesgo mira los últimos 30.
CREATE TABLE exposiciones (
    id              UUID          PRIMARY KEY,
    -- Las ofertas de una misma consulta comparten consulta_id: es una recomendación.
    consulta_id     UUID          NOT NULL,
    registrada_en   TIMESTAMPTZ   NOT NULL,
    origen          VARCHAR(20)   NOT NULL,
    motor           VARCHAR(20)   NOT NULL,
    oferta_id       VARCHAR(50)   NOT NULL,
    prestador_id    UUID          NOT NULL,
    posicion        INTEGER       NOT NULL,
    CONSTRAINT exposiciones_origen CHECK (origen IN ('RECOMENDACION', 'ALTERNATIVAS')),
    CONSTRAINT exposiciones_motor CHECK (motor IN ('REGLAS', 'IA')),
    CONSTRAINT exposiciones_posicion CHECK (posicion >= 1)
);

CREATE INDEX exposiciones_por_fecha ON exposiciones (registrada_en);

-- Las alertas de sesgo del panel del administrador (RET-02, DN-43). A lo sumo una abierta por prestador: se actualiza
-- mientras siga por encima del umbral y se cierra cuando baja.
CREATE TABLE alertas_sesgo (
    id                       UUID           PRIMARY KEY,
    prestador_id             UUID           NOT NULL,
    indice                   NUMERIC(8, 2)  NOT NULL,
    porcentaje_apariciones   NUMERIC(5, 2)  NOT NULL,
    porcentaje_ofertas       NUMERIC(5, 2)  NOT NULL,
    apariciones              BIGINT         NOT NULL,
    recomendaciones          BIGINT         NOT NULL,
    abierta_en               TIMESTAMPTZ    NOT NULL,
    actualizada_en           TIMESTAMPTZ    NOT NULL,
    cerrada_en               TIMESTAMPTZ
);

CREATE UNIQUE INDEX alertas_sesgo_abierta ON alertas_sesgo (prestador_id) WHERE cerrada_en IS NULL;

-- Una revisión del sesgo por día (DN-43): la réplica que inserta la fila del día es la que la hace.
CREATE TABLE revisiones_sesgo (
    dia           DATE          PRIMARY KEY,
    revisada_en   TIMESTAMPTZ   NOT NULL
);
