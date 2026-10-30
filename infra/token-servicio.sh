#!/usr/bin/env bash
# Firma un token de servicio para llamar a mano a /internal/** del catálogo (DN-38), como lo haría el núcleo o la
# recomendación, con su clave privada de infra/.env. Vale 5 minutos. Sirve para la demostración y para diagnosticar;
# quien lo usa ya tiene las claves.
#   TOKEN=$(bash infra/token-servicio.sh nucleo)          # o recomendacion
set -euo pipefail

cd "$(dirname "$0")"
servicio="${1:?Uso: bash infra/token-servicio.sh nucleo|recomendacion}"
case "$servicio" in
  nucleo) variable=NUCLEO_JWT_CLAVE_PRIVADA; emisor=samarian-tour-nucleo ;;
  recomendacion) variable=RECOMENDACION_JWT_CLAVE_PRIVADA; emisor=samarian-tour-recomendacion ;;
  *) echo "Servicio desconocido: $servicio (nucleo o recomendacion)" >&2; exit 1 ;;
esac
clave="$(grep "^${variable}=" .env 2>/dev/null | tail -n 1 | cut -d= -f2-)"
[ -n "$clave" ] || { echo "Falta ${variable} en infra/.env: ejecuta bash infra/generar-claves.sh" >&2; exit 1; }

base64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

ahora=$(date +%s)
cabecera=$(printf '{"alg":"RS256","typ":"JWT"}' | base64url)
datos=$(printf '{"iss":"%s","sub":"%s","aud":["catalogo"],"iat":%d,"exp":%d}' \
  "$emisor" "$servicio" "$ahora" "$((ahora + 300))" | base64url)

pem=$(mktemp)
trap 'rm -f "$pem"' EXIT
{ echo "-----BEGIN PRIVATE KEY-----"; printf '%s' "$clave" | fold -w 64; echo; echo "-----END PRIVATE KEY-----"; } > "$pem"
firma=$(printf '%s.%s' "$cabecera" "$datos" | openssl dgst -sha256 -sign "$pem" -binary | base64url)
printf '%s.%s.%s\n' "$cabecera" "$datos" "$firma"
