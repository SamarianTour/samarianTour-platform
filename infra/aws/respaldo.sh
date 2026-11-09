#!/usr/bin/env bash
# Respaldo del piloto en el servidor (DN-46): la base del núcleo, la del catálogo, las fotos y una copia de infra/.env,
# en ~/respaldos/<fecha>. Sin la clave de cifrado de infra/.env, los nombres y WhatsApp de la base no se pueden leer
# (DN-28): por eso va en el mismo respaldo, que no debe quedar en un lugar público.
#   bash infra/aws/respaldo.sh
# Para llevarlo al equipo propio:
#   scp -r -i <llave.pem> ubuntu@<ip>:respaldos/<fecha> .
set -euo pipefail

cd "$(dirname "$0")/.."
compose() { docker compose -f docker-compose.yml -f docker-compose.aws.yml "$@"; }
DESTINO="$HOME/respaldos/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DESTINO"
chmod 700 "$HOME/respaldos" "$DESTINO"

valor() { grep "^$1=" .env | tail -n 1 | cut -d= -f2- || true; }
POSTGRES_USUARIO="$(valor POSTGRES_USUARIO)"

echo "Núcleo (PostgreSQL)"
compose exec -T postgres pg_dump -U "${POSTGRES_USUARIO:-nucleo}" -d nucleo_db --format=custom \
  > "$DESTINO/nucleo_db.dump"

echo "Catálogo (MongoDB)"
compose exec -T mongo mongodump --db catalogo_db --archive --gzip > "$DESTINO/catalogo_db.archive.gz"

echo "Fotos (MinIO)"
# El volumen se lee desde un contenedor aparte: la imagen de MinIO no trae tar. El nombre lleva el del proyecto.
docker run --rm -v samarian-tour_minio-data:/data:ro alpine:3.20 tar -czf - -C /data . > "$DESTINO/fotos.tar.gz"

cp .env "$DESTINO/env"
chmod 600 "$DESTINO"/*

echo
du -sh "$DESTINO"
echo "Para restaurar: pg_restore --clean -d nucleo_db, mongorestore --drop --archive --gzip y tar -xzf en el volumen."
