-- Las abiertas de cada prestador: cada minuto se cierran las de los que perdieron el RNT (RF-77, DN-36).
CREATE INDEX solicitudes_abiertas_por_prestador ON solicitudes (prestador_id)
    WHERE estado IN ('PENDIENTE', 'ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO');
