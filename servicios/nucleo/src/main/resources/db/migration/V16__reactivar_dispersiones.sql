-- Cuántas veces una dispersión fallida volvió a quedar pendiente porque el prestador cambió su cuenta de cobro
-- (RF-54, DN-44). La causa del último fallo se conserva en ultima_causa.
ALTER TABLE dispersiones ADD COLUMN reactivaciones INTEGER NOT NULL DEFAULT 0;
