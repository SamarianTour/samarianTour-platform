#!/usr/bin/env bash
# Genera las claves del núcleo en infra/.env, que no se versiona (DN-28). Agrega solo las que falten: correrlo otra vez
# no cambia una clave que ya existe, porque los datos cifrados con ella dejarían de leerse.
#   bash infra/generar-claves.sh
set -euo pipefail

cd "$(dirname "$0")"
[ -f .env ] || cp .env.example .env

agregar() {
  local nombre="$1" valor="$2"
  if grep -q "^${nombre}=" .env; then
    echo "${nombre}: ya estaba, no se cambia"
  else
    printf '\n%s=%s\n' "$nombre" "$valor" >> .env
    echo "${nombre}: generada"
  fi
}

# Cifrado en reposo del nombre y el WhatsApp de las cuentas (RNF-08): 32 bytes aleatorios en Base64.
agregar NUCLEO_CIFRADO_CLAVE "$(openssl rand -base64 32)"

# Clave privada RSA de 2048 bits que firma el token de acceso (DN-29): PKCS#8 en Base64, en una sola línea. genpkey
# la escribe en PEM PKCS#8; pkcs8 la pasa a DER sin cambiar el formato (con -outform DER, genpkey usaría PKCS#1).
agregar NUCLEO_JWT_CLAVE_PRIVADA "$(openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null \
  | openssl pkcs8 -topk8 -nocrypt -outform DER | openssl base64 -A)"
