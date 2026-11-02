-- La categoría del prestador por su tamaño (RF-72, RF-89), copiada al crear la solicitud como la zona y el tipo de
-- oferta (DN-41): si el prestador crece o se achica, las solicitudes anteriores conservan la que tenía.

ALTER TABLE solicitudes
    ADD COLUMN categoria_prestador VARCHAR(20),
    ADD CONSTRAINT solicitudes_categoria_prestador
        CHECK (categoria_prestador IN ('GUIA_INDEPENDIENTE', 'MICRO_PEQUENO', 'MEDIANO', 'GRANDE'));

-- Las que ya existían toman la categoría actual de su prestador, con la regla de ReglaCategoria. Si el prestador no
-- está registrado, queda vacía.
UPDATE solicitudes s
SET categoria_prestador = CASE
        WHEN p.tipo = 'GUIA_INDEPENDIENTE' THEN 'GUIA_INDEPENDIENTE'
        WHEN p.numero_empleados IS NULL OR p.numero_empleados <= 50 THEN 'MICRO_PEQUENO'
        WHEN p.numero_empleados <= 200 THEN 'MEDIANO'
        ELSE 'GRANDE'
    END
FROM prestadores p
WHERE p.id = s.prestador_id;

-- Los indicadores cuentan las solicitudes enviadas en un periodo (RF-85 a RF-89).
CREATE INDEX solicitudes_por_creacion ON solicitudes (creada_en);
