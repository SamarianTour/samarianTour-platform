#!/usr/bin/env bash
# Pide el primer certificado de Let's Encrypt para el dominio de infra/.env y levanta el piloto con HTTPS (DN-46). Se
# corre una vez, en el servidor, cuando el registro A del dominio ya apunta a la IP elástica; las renovaciones las
# hace el contenedor certbot solo.
#   bash infra/aws/primer-certificado.sh            (certificado real)
#   PRUEBA=1 bash infra/aws/primer-certificado.sh   (el entorno de prueba de Let's Encrypt, sin su límite semanal)
#
# Nginx no arranca sin un certificado, y Let's Encrypt no lo emite sin Nginx sirviendo su desafío por el 80. Por eso
# primero deja uno provisional, autofirmado, levanta todo, lo cambia por el real y recarga Nginx.
set -euo pipefail

cd "$(dirname "$0")/.."
[ -f .env ] || { echo "Falta infra/.env: primero bash infra/generar-claves.sh" >&2; exit 1; }

valor() { grep "^$1=" .env | tail -n 1 | cut -d= -f2- || true; }
DOMINIO="$(valor DOMINIO)"
CORREO="$(valor CORREO_CERTIFICADO)"
[ -n "$DOMINIO" ] || { echo "Falta DOMINIO=... en infra/.env" >&2; exit 1; }

compose() { docker compose -f docker-compose.yml -f docker-compose.aws.yml "$@"; }
VIVO=/etc/letsencrypt/live/samarian

if compose run --rm --entrypoint sh certbot -c "[ -f /etc/letsencrypt/renewal/samarian.conf ]"; then
  echo "Ya hay un certificado: lo renueva el contenedor certbot. Para pedir otro, borra el volumen certbot-certs."
  exit 0
fi

provisional() {
  compose run --rm --entrypoint sh certbot -c "mkdir -p $VIVO && openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout $VIVO/privkey.pem -out $VIVO/fullchain.pem -subj /CN=provisional 2>/dev/null"
}

echo "1. Certificado provisional, para que Nginx pueda arrancar"
provisional

echo "2. El piloto con Nginx en el 80 y el 443 (la primera vez construye las imágenes: unos minutos)"
compose up -d --build

echo "3. El certificado de Let's Encrypt para $DOMINIO"
compose run --rm --entrypoint sh certbot -c "rm -rf $VIVO /etc/letsencrypt/archive/samarian"
CUENTA=(--register-unsafely-without-email)
[ -z "$CORREO" ] || CUENTA=(--email "$CORREO" --no-eff-email)
PRUEBA_FLAG=""
[ "${PRUEBA:-0}" = "1" ] && PRUEBA_FLAG="--test-cert"
if ! compose run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot --cert-name samarian \
  -d "$DOMINIO" "${CUENTA[@]}" --agree-tos --non-interactive $PRUEBA_FLAG; then
  # Deja otra vez el provisional, para que Nginx pueda reiniciarse mientras se corrige el DNS o el grupo de seguridad.
  provisional
  echo "Let's Encrypt no emitió el certificado. Revisa que $DOMINIO apunte a esta IP y que el 80 esté abierto." >&2
  exit 1
fi

echo "4. Nginx toma el certificado nuevo"
compose exec nginx nginx -s reload

echo
echo "Listo: https://$DOMINIO"
