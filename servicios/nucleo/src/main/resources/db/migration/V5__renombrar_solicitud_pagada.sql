-- El aviso al prestador de una solicitud Pagada (RF-57) se llamaba RESERVA_PAGADA. En este proyecto el turista no
-- reserva: envía una solicitud de disponibilidad (glosario del SRS). Ahora es SOLICITUD_PAGADA, la misma plantilla que
-- recibe el turista; el destinatario decide qué datos lleva. Las que ya estaban guardadas se renombran para que se
-- puedan leer y enviar.

UPDATE notificaciones_solicitud SET tipo = 'SOLICITUD_PAGADA' WHERE tipo = 'RESERVA_PAGADA';
