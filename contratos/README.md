# Contratos

API first: los contratos OpenAPI se escriben antes del código de cada servicio.

| Archivo | Contenido | Estado |
|---|---|---|
| `catalogo.openapi.yaml` | API del catálogo: búsqueda, ficha, zonas, ofertas del prestador, opiniones, moderación, avisos internos y consultas internas de ofertas | Endpoints del prototipo |
| `nucleo.openapi.yaml` | API del núcleo: seguridad por rol y errores comunes; las cuentas; la moderación y la sincronización del RNT del administrador; el registro, el WhatsApp y la cuenta de cobro del prestador; las solicitudes de disponibilidad del turista y del operador, y los pagos: la sesión de cobro y los intentos del turista, el webhook de la pasarela, la comisión y los pagos tardíos del administrador | Cuentas, prestadores, moderación, RNT, solicitudes y pagos |
| `recomendacion.openapi.yaml` | API pública de la recomendación: `POST /recomendaciones` (de 3 a 5 ofertas explicadas, por reglas o con IA), `GET /zonas/{zonaId}/alertas` y `GET /alertas` (alertas del destino) y `GET /ofertas/{ofertaId}/alternativas` (alternativas por alta afluencia) | Endpoints del prototipo |
| `internos/` | Avisos de la bandeja de salida, consulta interna de ofertas y registro de exposición | Pendiente |

**Seguridad.** El catálogo y el núcleo declaran el esquema `tokenAcceso` (`Authorization: Bearer`, el JWT del núcleo) y lo exigen en las rutas de cada rol, con `security: [tokenAcceso: []]` en cada operación; las demás no lo piden (DN-37). Los parámetros `X-Usuario-Id`, `X-Rol` y `X-Tipo-Prestador` del prototipo ya no existen. Las rutas de la recomendación son públicas. Las internas del catálogo exigen `tokenServicio`, el token del servicio que llama (DN-38).

Los avisos internos del núcleo al catálogo (`POST /internal/avisos` para las ofertas y `POST /internal/opiniones/avisos` para las opiniones) y las consultas internas (`GET /internal/ofertas/{id}`, `GET /internal/moderacion/ofertas/{id}` y `GET /internal/moderacion/opiniones/{id}` para el núcleo, y `GET /internal/ofertas` para la recomendación) están por ahora dentro de `catalogo.openapi.yaml`; pasan a `internos/` cuando se escriban los contratos del núcleo y de la recomendación.

El registro de exposición de la recomendación (RET-02) todavía no tiene contrato: mientras el núcleo no tenga esa ruta, la recomendación lo escribe en su log (DR-08 de `docs/decisiones-recomendacion.md`).

Una prueba en cada servicio (`ContratoCatalogoTest`, `ContratoNucleoIT` y `ContratoRecomendacionTest`) compara las rutas del contrato con las de los controladores: si una ruta existe en un lado y no en el otro, el build falla. Las del catálogo y la recomendación comparan además los campos de cada esquema con los de su DTO, y la de la recomendación, los valores de los enum.
