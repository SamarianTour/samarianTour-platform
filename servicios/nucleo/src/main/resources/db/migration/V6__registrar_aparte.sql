-- El aparte temporal del cupo que el operador acuerda con el prestador (RF-38). Su vigencia es la ventana de
-- confirmación y pago, desde la transición a Atendida (RF-55). vence_en se vacía al cerrar la solicitud; el aparte
-- queda registrado para el historial y los reportes. Vacío si nunca se atendió.

ALTER TABLE solicitudes ADD COLUMN aparte_hasta TIMESTAMPTZ;

-- Las que ya estaban atendidas o más adelante tienen la ventana todavía en vence_en si siguen abiertas.
UPDATE solicitudes SET aparte_hasta = vence_en
WHERE precio_base IS NOT NULL AND estado IN ('ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO');
