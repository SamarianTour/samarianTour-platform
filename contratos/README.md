# Contratos

API first: los contratos OpenAPI se escriben antes del código de cada servicio.

| Archivo | Contenido | Estado |
|---|---|---|
| `catalogo.openapi.yaml` | API del catálogo: búsqueda, ficha, zonas, ofertas del prestador, opiniones, moderación, avisos internos y consultas internas de ofertas | Endpoints del prototipo |
| `nucleo.openapi.yaml` | API pública del núcleo | Pendiente |
| `recomendacion.openapi.yaml` | API pública de la recomendación | Pendiente |
| `internos/` | Avisos de la bandeja de salida, consulta interna de ofertas y registro de exposición | Pendiente |

Los avisos internos del núcleo al catálogo (`POST /internal/avisos` para las ofertas y `POST /internal/opiniones/avisos` para las opiniones) y las consultas internas (`GET /internal/ofertas/{id}`, `GET /internal/moderacion/ofertas/{id}` y `GET /internal/moderacion/opiniones/{id}` para el núcleo, y `GET /internal/ofertas` para la recomendación) están por ahora dentro de `catalogo.openapi.yaml`; pasan a `internos/` cuando se escriban los contratos del núcleo y de la recomendación.

Una prueba del catálogo (`ContratoCatalogoTest`) compara las rutas del contrato con las de los controladores: si una ruta existe en un lado y no en el otro, el build falla.
