#!/usr/bin/env bash
# Prepara una instancia EC2 nueva con Ubuntu 24.04 para el piloto (DN-46): Docker con Compose, 2 GB de swap y el
# usuario en el grupo docker. Se corre una sola vez, en el servidor; correrlo otra vez no cambia lo que ya está.
#   bash infra/aws/preparar-servidor.sh
set -euo pipefail

if [ "$(id -u)" -eq 0 ]; then
  echo "Córrelo con el usuario normal (ubuntu), no como root: el script usa sudo donde hace falta." >&2
  exit 1
fi

# Docker Engine y el plugin de Compose, del repositorio oficial de Docker: el paquete de Ubuntu trae un Compose viejo
# que no entiende !override, que usa docker-compose.aws.yml.
if ! command -v docker >/dev/null; then
  sudo apt-get update
  sudo apt-get install -y ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc]" \
    "https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
  sudo apt-get update
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  echo "docker: instalado"
else
  echo "docker: ya estaba"
fi
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

# 2 GB de swap: la t3.medium tiene 4 GB, y la primera construcción de las imágenes (Maven y Angular) más los
# servicios corriendo pasan de ahí. Con swappiness 10 solo se usa cuando de verdad falta memoria.
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
  echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-samarian.conf >/dev/null
  sudo sysctl -p /etc/sysctl.d/99-samarian.conf >/dev/null
  echo "swap: 2 GB creados"
else
  echo "swap: ya estaba"
fi

# Los logs de los contenedores crecen sin límite por defecto; con esto, cada uno guarda a lo sumo 3 archivos de 10 MB.
if [ ! -f /etc/docker/daemon.json ]; then
  echo '{"log-driver": "json-file", "log-opts": {"max-size": "10m", "max-file": "3"}}' \
    | sudo tee /etc/docker/daemon.json >/dev/null
  sudo systemctl restart docker
  echo "logs: limitados a 30 MB por contenedor"
fi

echo
echo "Listo. Cierra la sesión SSH y vuelve a entrar para usar docker sin sudo; después sigue con el README."
