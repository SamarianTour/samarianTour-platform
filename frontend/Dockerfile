# Imagen del frontend: Nginx con la aplicación Angular ya compilada.
# El contexto de construcción es la carpeta frontend/:
#   docker build -t samarian-tour/frontend frontend

FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Los tipos de la API (src/app/core/api/catalogo-api.ts) están versionados, así que aquí no hace falta el contrato.
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist/samarian-tour/browser /usr/share/nginx/html
# La configuración de Nginx no va en la imagen: Docker Compose monta infra/nginx/nginx.conf.
EXPOSE 8080
