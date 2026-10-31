-- Correcciones del procesamiento de pagos (DN-20 a DN-22): que un elemento que falla no bloquee a los demás, que el
-- abandono respete la gracia de RF-55 y que un aprobado por otro monto quede para revisión.

-- Un evento que falla por un error inesperado se aplaza con espera creciente y, al quinto fallo, queda FALLIDO para
-- revisarlo a mano: así nunca se toma de primero en cada corrida ni bloquea a los que llegan después (DN-21).
ALTER TABLE eventos_pasarela
    ADD COLUMN procesamiento          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN intentos_procesamiento INTEGER      NOT NULL DEFAULT 0,
    -- El tipo y el mensaje de la excepción; nunca datos del turista.
    ADD COLUMN ultimo_error           VARCHAR(500),
    ADD COLUMN siguiente_intento_en   TIMESTAMPTZ,
    -- Cuándo se avisó al administrador de un evento fallido, y la última vez que se intentó sin lograrlo.
    ADD COLUMN aviso_enviado_en       TIMESTAMPTZ,
    ADD COLUMN aviso_intentado_en     TIMESTAMPTZ;

UPDATE eventos_pasarela SET procesamiento = 'PROCESADO' WHERE procesado_en IS NOT NULL;
UPDATE eventos_pasarela SET siguiente_intento_en = recibido_en;

ALTER TABLE eventos_pasarela
    ALTER COLUMN siguiente_intento_en SET NOT NULL,
    ADD CONSTRAINT eventos_pasarela_procesamiento CHECK (procesamiento IN ('PENDIENTE', 'PROCESADO', 'FALLIDO'));

-- La cola: los pendientes por la hora de su próximo intento.
DROP INDEX eventos_pasarela_sin_procesar;
CREATE INDEX eventos_pasarela_por_procesar ON eventos_pasarela (siguiente_intento_en)
    WHERE procesamiento = 'PENDIENTE';
CREATE INDEX eventos_pasarela_fallidos_sin_aviso ON eventos_pasarela (aviso_intentado_en NULLS FIRST)
    WHERE procesamiento = 'FALLIDO' AND aviso_enviado_en IS NULL;

-- Cuándo se abandona un intento sin resultado: a los 30 min de abierto, o al terminar la gracia de 30 min si la
-- ventana de la solicitud vence antes (DN-20, RF-55). Los que ya existen conservan la regla anterior.
ALTER TABLE intentos_pago ADD COLUMN abandonar_en TIMESTAMPTZ;
UPDATE intentos_pago SET abandonar_en = creado_en + INTERVAL '30 minutes';
ALTER TABLE intentos_pago ALTER COLUMN abandonar_en SET NOT NULL;

DROP INDEX intentos_pago_por_abandonar;
CREATE INDEX intentos_pago_por_abandonar ON intentos_pago (abandonar_en) WHERE estado = 'EN_CURSO';

-- Por qué un pago aprobado quedó para revisión: llegó tarde, o la pasarela aprobó otro monto (DN-22, RF-49).
ALTER TABLE pagos_tardios
    ADD COLUMN motivo             VARCHAR(20) NOT NULL DEFAULT 'PAGO_TARDIO',
    ADD COLUMN aviso_intentado_en TIMESTAMPTZ,
    ADD CONSTRAINT pagos_tardios_motivo CHECK (motivo IN ('PAGO_TARDIO', 'MONTO_DISTINTO'));
ALTER TABLE pagos_tardios ALTER COLUMN motivo DROP DEFAULT;

-- Un aviso que no sale pasa al final de la fila: el de intento más antiguo va primero (DN-22, DN-24).
DROP INDEX pagos_tardios_sin_aviso;
CREATE INDEX pagos_tardios_sin_aviso ON pagos_tardios (aviso_intentado_en NULLS FIRST)
    WHERE aviso_enviado_en IS NULL;

ALTER TABLE dispersiones ADD COLUMN aviso_intentado_en TIMESTAMPTZ;

DROP INDEX dispersiones_fallidas_sin_aviso;
CREATE INDEX dispersiones_fallidas_sin_aviso ON dispersiones (aviso_intentado_en NULLS FIRST)
    WHERE estado = 'FALLIDA' AND aviso_enviado_en IS NULL;
