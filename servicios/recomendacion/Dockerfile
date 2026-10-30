# Contexto de construcción: raíz del repositorio (el módulo necesita el pom padre).
#   docker build -f servicios/recomendacion/Dockerfile .

# --- Etapa 1: compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo los pom, para que la capa de dependencias se reutilice mientras no cambien.
COPY pom.xml .
COPY libs/seguridad-jwt/pom.xml libs/seguridad-jwt/
COPY servicios/catalogo/pom.xml servicios/catalogo/
COPY servicios/nucleo/pom.xml servicios/nucleo/
COPY servicios/recomendacion/pom.xml servicios/recomendacion/
RUN mvn -B -q -pl servicios/recomendacion -am dependency:go-offline

# La validación del token, común a los tres servicios (DN-37).
COPY libs/seguridad-jwt/src libs/seguridad-jwt/src
COPY servicios/recomendacion/src servicios/recomendacion/src
# Las pruebas corren en CI.
RUN mvn -B -q -pl servicios/recomendacion -am -DskipTests package

# --- Etapa 2: ejecución ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/servicios/recomendacion/target/recomendacion-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]
