-- Los campos propios de cada tipo de oferta en la solicitud (RF-30). La actividad y el paquete ya tenían el horario
-- de salida; el alojamiento pide la fecha de salida y el número de unidades, y el restaurante, la hora de la reserva.
-- Que cada tipo traiga los suyos lo valida el dominio (CamposDelTipo).

ALTER TABLE solicitudes
    -- Solo restaurante.
    ADD COLUMN hora_reserva      TIME,
    -- Solo alojamiento.
    ADD COLUMN fecha_salida      DATE,
    ADD COLUMN numero_unidades   INTEGER,
    ADD CONSTRAINT solicitudes_fecha_salida CHECK (fecha_salida IS NULL OR fecha_salida > fecha_inicio),
    ADD CONSTRAINT solicitudes_numero_unidades CHECK (numero_unidades IS NULL OR numero_unidades >= 1);

-- RF-31 con la hora que aplica a cada tipo: el horario de salida en actividad y paquete, la hora de la reserva en
-- restaurante. El alojamiento no tiene hora: con NULLS NOT DISTINCT se compara solo por la fecha de inicio. Cada
-- oferta es de un solo tipo, así que las dos horas nunca se mezclan para la misma oferta.
DROP INDEX solicitudes_no_duplicada;

CREATE UNIQUE INDEX solicitudes_no_duplicada
    ON solicitudes (turista_id, oferta_id, fecha_inicio, hora_salida, hora_reserva) NULLS NOT DISTINCT
    WHERE estado IN ('PENDIENTE', 'ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO');
