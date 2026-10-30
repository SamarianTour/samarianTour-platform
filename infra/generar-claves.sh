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

# La pública que corresponde a una privada que ya está en .env: X.509 en Base64, de una línea. Se calcula siempre, así
# que también completa un .env creado antes y nunca queda desalineada si la privada cambia.
derivar_publica() {
  local privada_nombre="$1" publica_nombre="$2" privada publica
  privada="$(grep "^${privada_nombre}=" .env | tail -n 1 | cut -d= -f2-)"
  publica="$(printf '%s' "$privada" | openssl base64 -d -A | openssl pkey -inform DER -pubout -outform DER \
    | openssl base64 -A)"
  sed -i "/^${publica_nombre}=/d" .env
  [ -z "$(tail -c 1 .env)" ] || echo >> .env
  printf '%s=%s\n' "$publica_nombre" "$publica" >> .env
  echo "${publica_nombre}: derivada de la privada"
}

# Una clave privada RSA de 2048 bits: PKCS#8 en Base64, en una sola línea. genpkey la escribe en PEM PKCS#8; pkcs8 la
# pasa a DER sin cambiar el formato (con -outform DER, genpkey usaría PKCS#1).
clave_rsa() {
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null \
    | openssl pkcs8 -topk8 -nocrypt -outform DER | openssl base64 -A
}

# Cifrado en reposo del nombre y el WhatsApp de las cuentas (RNF-08): 32 bytes aleatorios en Base64.
agregar NUCLEO_CIFRADO_CLAVE "$(openssl rand -base64 32)"

# El núcleo firma con su privada el token de acceso (DN-29) y su token de servicio (DN-38); con la pública, el catálogo
# y la recomendación validan el token de acceso, y el catálogo, el de servicio del núcleo (DN-37).
agregar NUCLEO_JWT_CLAVE_PRIVADA "$(clave_rsa)"
derivar_publica NUCLEO_JWT_CLAVE_PRIVADA NUCLEO_JWT_CLAVE_PUBLICA

# La recomendación firma su token de servicio con su propia privada, y el catálogo lo valida con la pública (DN-38).
agregar RECOMENDACION_JWT_CLAVE_PRIVADA "$(clave_rsa)"
derivar_publica RECOMENDACION_JWT_CLAVE_PRIVADA RECOMENDACION_JWT_CLAVE_PUBLICA
