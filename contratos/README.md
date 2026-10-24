# Contratos

API first: los contratos OpenAPI se escriben antes del código de cada servicio.

| Archivo | Contenido | Estado |
|---|---|---|
| `catalogo.openapi.yaml` | API del catálogo: búsqueda, ficha, zonas, ofertas del prestador, opiniones, moderación, avisos internos y consultas internas de ofertas | Endpoints del prototipo |
| `nucleo.openapi.yaml` | API pública del núcleo | Pendiente |
| `recomendacion.openapi.yaml` | API pública de la recomendación: `POST /recomendaciones`, de 3 a 5 ofertas explicadas, por reglas o con IA | Endpoints del prototipo |
| `internos/` | Avisos de la bandeja de salida, consulta interna de ofertas y registro de exposición | Pendiente |

Los avisos internos del núcleo al catálogo (`POST /internal/avisos` para las ofertas y `POST /internal/opiniones/avisos` para las opiniones) y las consultas internas (`GET /internal/ofertas/{id}`, `GET /internal/moderacion/ofertas/{id}` y `GET /internal/moderacion/opiniones/{id}` para el núcleo, y `GET /internal/ofertas` para la recomendación) están por ahora dentro de `catalogo.openapi.yaml`; pasan a `internos/` cuando se escriban los contratos del núcleo y de la recomendación.

El registro de exposición de la recomendación (RET-02) todavía no tiene contrato: mientras no exista el núcleo, la recomendación lo escribe en su log (DR-08 de `docs/decisiones-recomendacion.md`).

Una prueba en cada servicio (`ContratoCatalogoTest` y `ContratoRecomendacionTest`) compara las rutas del contrato con las de los controladores, y los campos de cada esquema con los de su DTO: si algo existe en un lado y no en el otro, el build falla. La de la recomendación compara además los valores de los enum.
