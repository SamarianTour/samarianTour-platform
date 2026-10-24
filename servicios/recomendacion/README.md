# recomendacion

Servicio de recomendación (RF-26, RF-27): recibe lo que busca el turista y devuelve de 3 a 5 ofertas visibles del catálogo, cada una con la explicación del criterio usado. Sin base de datos propia. Las decisiones están en [docs/decisiones-recomendacion.md](../../docs/decisiones-recomendacion.md) y el contrato en [contratos/recomendacion.openapi.yaml](../../contratos/recomendacion.openapi.yaml).

- **Endpoint:** `POST /api/recomendacion/recomendaciones`, público. POST para que el texto del turista no quede en la URL (DR-09).
- **Motores:** por reglas, siempre disponible, o con IA, si `recomendacion.ia.habilitada=true` y hay key (DR-01). Si la IA falla, responde el de reglas sin error para el turista (DR-05, DR-06).
- **Criterios:** solo los explícitos: tipo de oferta, tipo de actividad, zona, accesibilidad, esfuerzo y precio. Nunca calificaciones, comisión ni identidad del prestador (DR-03).
- **Fuera de esta rama:** las alertas del IDEAM (RF-28) y las alternativas por alta afluencia (RF-29).

## Paquetes

Base: `co.edu.unimagdalena.samariantour.recomendacion`, con el módulo `recomendacion` y `compartido`, igual que el catálogo (módulo 09 del diagrama de paquetes):

| Capa | Contenido |
|---|---|
| `dominio/modelo` | `Preferencias`, `OfertaCandidata`, `Recomendacion`, `Explicacion`, `CriterioAfinidad`, `MotorRecomendacion`, `PropuestaIA`, `RegistroExposicion` |
| `dominio/politica` | `ReglaCriteriosExplicitos`, `RelevanciaTexto`, `PoliticaRotacion`, `ValidacionRespuestaIA` |
| `dominio/puerto` | Entrada: `RecomendarOfertasUseCase`. Salida: `CatalogoConsultaPort`, `RecommendationPort`, `ExposicionPort` |
| `aplicacion` | `RecomendacionService` |
| `infraestructura/entrada/rest` | `RecomendacionController`, DTO y manejador de errores |
| `infraestructura/salida` | `CatalogoRestClientAdapter` (con caché), `ProveedorIAAdapter`, `RecomendadorPorReglasAdapter`, `ExposicionLogAdapter` |
| `compartido/http` | Clientes HTTP con tiempo máximo |

`ArquitecturaTest` aplica 7 de las 9 reglas del catálogo; no aplican la de documentos de MongoDB ni la del control de concurrencia, porque no hay base de datos.

## Configuración

| Propiedad | Por defecto | Para qué |
|---|---|---|
| `recomendacion.catalogo.url` | `http://localhost:8080/api/catalogo` | Base del catálogo; en Docker Compose, `http://catalogo:8080/api/catalogo` |
| `recomendacion.catalogo.timeout` | `3s` | Corte por petición al catálogo |
| `recomendacion.catalogo.vigencia-cache` | `60s` | Cada cuánto se vuelve a leer el catálogo (DR-07) |
| `recomendacion.ia.habilitada` | `false` | Motor con IA (QAS-06) |
| `recomendacion.ia.url` | `https://api.groq.com/openai/v1` | Cualquier API compatible con la de OpenAI |
| `recomendacion.ia.modelo` | `openai/gpt-oss-120b` | Modelo del proveedor |
| `recomendacion.ia.api-key` | — | Solo por variable de entorno: `RECOMENDACION_IA_API_KEY` |
| `recomendacion.ia.timeout` | `6s` | Corte de la llamada al proveedor |
| `recomendacion.ia.candidatas` | `10` | Tamaño de la lista corta que se envía al modelo (DR-04) |
| `recomendacion.ia.esfuerzo-razonamiento` | `low` | `reasoning_effort`; vacío para modelos que no razonan |
| `resilience4j.circuitbreaker.instances.ia.*` | 3 fallas seguidas, 60 s abierto | Corte de circuito (DR-06) |

## Correr y probar

```bash
mvn -pl servicios/recomendacion test                     # sin Docker; ninguna prueba llama a Groq ni al catálogo real
mvn -pl servicios/recomendacion spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

Fuera de Docker usa el puerto 8082 para no chocar con el catálogo, que usa el 8080. En el piloto corre en Docker Compose detrás de Nginx; el [README principal](../../README.md) explica cómo encender la IA con `infra/.env` y cómo cambiar de proveedor sin recompilar.
