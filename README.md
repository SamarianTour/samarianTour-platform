# Samarian Tour

Plataforma de gestión sostenible del turismo en Santa Marta. Monorepo Maven multimódulo con arquitectura basada en servicios (`nucleo`, `catalogo`, `recomendacion`) y arquitectura hexagonal dentro de cada módulo de cada servicio (ADR-02).

> En construcción. Etapa actual: estructura y herramientas. El flujo de demostración y la trazabilidad con los ADR y QAS se agregan cuando exista la lógica del catálogo.

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `servicios/catalogo` | Servicio de catálogo (MongoDB). Módulos `catalogo` y `opiniones`, más `compartido`. |
| `servicios/nucleo` | Núcleo transaccional (PostgreSQL). Esqueleto. |
| `servicios/recomendacion` | Recomendación (sin base de datos propia). Esqueleto. |
| `libs/seguridad-jwt` | Validación del JWT RS256 y roles, compartida por los tres servicios. Pendiente. |
| `contratos/` | Contratos OpenAPI (API first). Pendiente. |
| `frontend/` | Angular. Fuera del alcance del prototipo. |
| `infra/` | Docker Compose, Nginx y precarga de MongoDB. |
| `docs/` | Documento de diseño, diagrama de paquetes y ADR. |

## Estructura de paquetes de un servicio

Cada servicio se divide en módulos de dominio y cada módulo es hexagonal por dentro:

```
co.edu.unimagdalena.samariantour.catalogo
├── CatalogoApplication
├── compartido/                 transversal; no importa ningún módulo
├── catalogo/                   módulo
│   ├── api/                    (solo si otro módulo lo usa) lo único visible para otros módulos
│   ├── dominio/
│   │   ├── modelo/             Java puro
│   │   ├── politica/
│   │   ├── excepcion/
│   │   └── puerto/
│   │       ├── entrada/        un caso de uso por interfaz (+ comando/)
│   │       └── salida/
│   ├── aplicacion/             implementa los casos de uso
│   └── infraestructura/
│       ├── entrada/rest/       controladores, dto/, mapper/
│       └── salida/persistencia/ documentos Mongo, repositorios y mappers
└── opiniones/                  módulo (fuera del alcance del prototipo)
```

Las reglas se verifican con ArchUnit en cada build (`ArquitecturaTest`):

1. `dominio` no importa Spring, Jakarta, Jackson, MongoDB, `aplicacion` ni `infraestructura`.
2. `aplicacion` solo depende de `dominio` (se permiten `@Service` y `@Transactional`).
3. Ningún paquete depende de `infraestructura`.
4. Un módulo solo importa el paquete `api` de otro módulo.
5. Los módulos no forman ciclos.
6. `compartido` no importa ningún módulo.
7. Los controladores solo llaman puertos de entrada; nunca repositorios ni puertos de salida.
8. Los documentos Mongo y los repositorios viven en `infraestructura/salida/persistencia`.

## Requisitos

Java 21, Maven 3.9 y Docker (las pruebas de integración usan Testcontainers).

## Compilar y probar

```bash
mvn test      # unitarias y ArchUnit (no necesita Docker)
mvn verify    # además, las de integración (*IT) con MongoDB en Testcontainers
```

## Levantar el piloto

```bash
docker compose -f infra/docker-compose.yml up --build
```

Nginx queda en `http://localhost:8080` y enruta `/api/catalogo/**` hacia las 2 réplicas del catálogo. Cada respuesta trae el header `X-Instancia` con el hostname del contenedor que la atendió, y los errores salen como `application/problem+json` (RFC 7807). Las rutas `/api/catalogo/internal/**` no se exponen hacia afuera.

Actualizar solo el catálogo sin detener lo demás:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps catalogo
```

## Cómo contribuir

El flujo de ramas (GitFlow), los pull requests y la guía de mensajes de commit están en [CONTRIBUTING.md](CONTRIBUTING.md).
