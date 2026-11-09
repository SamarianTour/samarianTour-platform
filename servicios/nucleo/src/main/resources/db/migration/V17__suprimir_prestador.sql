-- Supresión de los datos del prestador (RF-13, DN-45). La fila se conserva, sin WhatsApp, como soporte de los pagos
-- que ya recibió; las consultas de los prestadores vigentes no la ven.
ALTER TABLE prestadores ADD COLUMN suprimido_en TIMESTAMPTZ;

-- El número de RNT queda libre cuando su prestador suprime sus datos: el mismo negocio puede volver a registrarse
-- con otra cuenta. El índice conserva el nombre de la restricción, que el adaptador reconoce (RNT_YA_REGISTRADO).
ALTER TABLE prestadores DROP CONSTRAINT prestadores_rnt_unico;
CREATE UNIQUE INDEX prestadores_rnt_unico ON prestadores (numero_rnt) WHERE suprimido_en IS NULL;
