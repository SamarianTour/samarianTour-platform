-- Bandeja de salida hacia el catálogo (DN-06). Cada aviso se guarda en la misma transacción que el cambio que lo
-- origina, y el publicador lo envía después: si el catálogo no responde, el cambio no se pierde ni se deshace.
CREATE TABLE bandeja_salida (
    -- El catálogo aplica cada aviso una sola vez por este identificador, aunque llegue repetido.
    id_aviso        VARCHAR(200)  PRIMARY KEY,
    -- A cuál de las dos entradas de avisos del catálogo va: la de ofertas o la de opiniones.
    destino         VARCHAR(20)   NOT NULL,
    cuerpo          JSONB         NOT NULL,
    -- PENDIENTE hasta que el catálogo lo recibe (ENVIADO) o lo rechaza con un 4xx (RECHAZADO).
    estado          VARCHAR(20)   NOT NULL,
    creado_en       TIMESTAMPTZ   NOT NULL,
    proximo_intento TIMESTAMPTZ   NOT NULL,
    intentos        INTEGER       NOT NULL,
    enviado_en      TIMESTAMPTZ,
    ultimo_error    VARCHAR(1000),
    CONSTRAINT bandeja_salida_destino CHECK (destino IN ('OFERTAS', 'OPINIONES')),
    CONSTRAINT bandeja_salida_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'RECHAZADO'))
);

-- Solo los pendientes, en el orden en que el publicador los toma.
CREATE INDEX bandeja_salida_pendientes ON bandeja_salida (proximo_intento, creado_en) WHERE estado = 'PENDIENTE';
