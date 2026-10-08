# Contexto de construcción: raíz del repositorio (el módulo necesita el pom padre).
#   docker build -f servicios/catalogo/Dockerfile .

# --- Etapa 1: compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo los pom, para que la capa de dependencias se reutilice mientras no cambien.
COPY pom.xml .
COPY servicios/catalogo/pom.xml servicios/catalogo/
COPY servicios/nucleo/pom.xml servicios/nucleo/
COPY servicios/recomendacion/pom.xml servicios/recomendacion/
RUN mvn -B -q -pl servicios/catalogo -am dependency:go-offline

COPY servicios/catalogo/src servicios/catalogo/src
# Las pruebas corren en CI (Testcontainers necesita Docker, que no hay dentro de la construcción).
RUN mvn -B -q -pl servicios/catalogo -am -DskipTests package

# --- Etapa 2: ejecución ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/servicios/catalogo/target/catalogo-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx384m", "-jar", "/app/app.jar"]
