# Decisiones del prototipo: ofertas del catálogo

Registro de las decisiones tomadas para el prototipo del servicio `catalogo` (gestión de ofertas del prestador, tipos ACTIVIDAD, PAQUETE, ALOJAMIENTO y RESTAURANTE). Complementa el diagrama de paquetes, el diagrama de clases y el diccionario de clases; donde los contradice, se indica en la sección final.

- **Estado:** vigente, octubre de 2026. Pendiente de rectificar contra el SRS cuando termine su revisión.
- **Rama:** `feature/catalogo-ofertas`.
- **Convención:** ✅ decisión que sale de un requisito o de una decisión anterior (se cita la fuente). 🆕 decisión nueva del equipo.

## Resumen

| # | Decisión | Origen |
|---|---|---|
| DC-01 | La identidad del prestador viene del token; en el prototipo, de headers | ✅ reparto Núcleo/Catálogo |
| DC-02 | Réplica `PrestadorCatalogo` con la habilitación RNT | ✅ RF-77, reparto Núcleo/Catálogo |
| DC-03 | Copia `rntPrestadorActivo` en cada oferta | 🆕 QAS-07 |
| DC-04 | Avisos del núcleo idempotentes y no expuestos | ✅ ADR-02, ADR-04 |
| DC-05 | Número de versión que siempre crece | 🆕 |
| DC-06 | La versión rechazada queda como pendiente | 🆕 |
| DC-07 | Editar crea una versión pendiente; la publicada sigue visible | ✅ RF-66 |
| DC-08 | Moderación idempotente | ✅ ADR-02 |
| DC-09 | Retiro definitivo | ✅ RF-65 |
| DC-10 | Qué puede publicar cada tipo de prestador | ✅ RF-65, RF-67 |
| DC-11 | Publicar y editar exigen el RNT activo | ✅ RF-79, reparto Núcleo/Catálogo |
| DC-12 | La zona debe estar activa | ✅ RF-84 |
| DC-13 | Rutas del prestador en `/prestador/**` | 🆕 |
| DC-14 | Paginación | 🆕 QAS-07 |
| DC-15 | Errores en ProblemDetail con código de motivo | 🆕 |
| DC-16 | Modelo documental de `ofertas` | ✅ ADR-03 |
| DC-17 | Índice parcial para la búsqueda | 🆕 QAS-07 |
| DC-18 | Zonas de la precarga | ✅ D-04 [SUPUESTO] |
| DC-19 | Validaciones de campo provisionales | 🆕 hasta cerrar el SRS |
| DC-20 | Stack del prototipo | 🆕 |
| DC-21 | Un servicio de aplicación por caso de uso | ✅ diagrama de paquetes |
| DC-22 | Unidad del precio de referencia por tipo de oferta | 🆕 RF-20 |
| DC-23 | Actividades que puede incluir un paquete | 🆕 RF-22, DC-09 |
| DC-24 | El filtro por tipo de actividad incluye los paquetes | 🆕 |
| DC-25 | El filtro por nivel de esfuerzo es "hasta el nivel indicado" | 🆕 RF-17 |
| DC-26 | Perfil público: solo la descripción es obligatoria | 🆕 RF-64, RS-03 |
| DC-27 | Perfiles de ejecución `memoria` y `demo` | 🆕 |
| DC-28 | Con los datos en memoria, cada réplica tiene su propio estado | 🆕 QAS-02 |
| DC-29 | Forma de las respuestas de la API | 🆕 RF-20, RF-24, RF-25 |
| DC-30 | Los identificadores del catálogo son `ObjectId` | ✅ modelo documental |
| DC-31 | La búsqueda de paquetes usa una agregación | 🆕 RF-22, QAS-07 |
| DC-32 | El servicio de recomendación no tiene base de datos | ✅ ADR-02, ADR-03 |

## Identidad y habilitación del prestador

**DC-01. La identidad viene del token.** El catálogo autoriza con `prestadorId`, rol y tipo de prestador tomados del JWT que emite el núcleo. En el prototipo no hay JWT todavía, así que los envían los headers `X-Prestador-Id` y `X-Tipo-Prestador`, marcados en el código como **sustituto del token**. Cuando exista `libs/seguridad-jwt`, solo cambia el adaptador de `compartido/seguridad` (`ResolutorIdentidad`). El tipo de prestador **no** se guarda en el catálogo, para no tener dos fuentes de verdad.
- Cualquiera puede enviar esos headers, así que solo se aceptan con el perfil `demo` (DC-27). Sin ese perfil, las rutas del prestador responden 401 `NO_AUTENTICADO`.
- El rol no se envía: las rutas `/prestador/**` solo las usa el prestador, y la autorización por rol llega con el JWT.

**DC-02. Réplica de la habilitación RNT.** `PrestadorCatalogo {prestadorId, rntActivo, version}` reemplaza en el catálogo a la bandeja de salida del núcleo y es la fuente de verdad para autorizar publicaciones.
- Un prestador del que aún no llega ningún aviso se trata como **no habilitado**. En la demostración, el aviso de habilitación va antes de publicar.
- `version` es un número por prestador que genera el núcleo, por ejemplo la columna de bloqueo optimista de su fila. Crece con cada cambio y se usa en vez de `ocurridoEn`, porque las réplicas del núcleo pueden tener relojes distintos. Se aplica un aviso solo si su versión es mayor que la guardada; así se descartan los avisos viejos que lleguen en desorden.

**DC-03. Copia en cada oferta.** `Oferta.rntPrestadorActivo` es una copia desnormalizada para que la búsqueda pública use solo el índice de ofertas: MongoDB no hace *joins* baratos y QAS-07 lo exige.
- Al llegar un aviso de habilitación: (1) se actualiza la réplica; (2) un `updateMany` actualiza las ofertas de ese `prestadorId`; (3) al final se marca el aviso como procesado. Si el proceso se cae a mitad, reprocesar el aviso deja todo correcto.
- Al **aprobar** una versión se vuelve a copiar la habilitación desde la réplica. Una oferta solo puede ser visible a partir de su aprobación, así que la copia que importa siempre está fresca. Esto cierra la carrera entre "publicar" y "aviso de RNT inactivo".

**DC-04. Avisos del núcleo.** Llegan a `POST /api/catalogo/internal/avisos`.
- Traen un `idAviso`. El catálogo consulta si ya fue procesado al inicio y lo marca al final. Si dos réplicas procesan el mismo aviso a la vez, las operaciones asignan valores absolutos y el segundo registro choca con el `_id` único y se ignora.
- Los avisos de habilitación traen la `version` de DC-02, y los de moderación el número de versión de DC-05.
- Cada aviso termina en uno de tres resultados: `APLICADO` (cambió el catálogo), `IGNORADO` (llegó tarde o ya no aplica: versión vieja, versión reemplazada, oferta inexistente o retirada) o `DUPLICADO` (ya se había procesado). En los tres casos el aviso queda registrado y el catálogo responde con éxito, para que el núcleo no lo reintente sin fin.
- Nginx responde 404 a `/api/catalogo/internal/**` desde afuera. Dentro de la red de Docker todavía no hay autenticación entre servicios; queda pendiente para cuando exista el JWT.

## Versiones y moderación

**DC-05. El número de versión siempre crece.** Cada `VersionOferta` tiene un `numero`, y el aviso de moderación lo nombra. El número crece también cuando una edición reemplaza a la pendiente: si se reutilizara, el aviso de una versión vieja aprobaría la nueva. Se calcula como el mayor entre el de la publicada y el de la pendiente, más uno; como el número más alto emitido siempre sigue vivo, nunca se repite.

**DC-06. La versión rechazada queda como pendiente**, con su `motivoRechazo`, hasta que el prestador edite. Esto respeta la regla de que toda oferta tiene al menos una versión, publicada o pendiente.

**DC-07. Editar crea una versión pendiente y la publicada sigue visible (RF-66).** La nueva versión reemplaza a la pendiente anterior, esté en moderación o rechazada, con número nuevo y sin el motivo de rechazo anterior. Al aprobarse, reemplaza a la publicada.
> Esta es la regla que se demuestra en la sustentación: `OfertaTest › Editar › [Sustentación]`.

**DC-08. La moderación es idempotente.** Aprobar o rechazar dos veces la misma versión no falla ni cambia nada. Un aviso cuyo número no coincide con la versión en moderación (porque ya se aplicó, fue reemplazada o la oferta está retirada) se marca como procesado y se ignora, para que el núcleo no lo reintente sin fin. Rechazar exige motivo.

## Reglas de publicación

**DC-09. El retiro es definitivo (RF-65).** RF-65 dice "publicar, editar y retirar" y no menciona reactivar.
- Retirar es un cambio de estado, nunca un borrado. Una oferta retirada no se puede editar e ignora los avisos de moderación.
- Retirar **sí** se permite con el RNT inactivo, porque no publica nada.
- RF-65 agrega que retirar no afecta las solicitudes en curso; eso lo garantiza el núcleo con su copia de la oferta.

**DC-10. Qué puede publicar cada tipo de prestador (`ReglaTipoOferta`).**

| Tipo de prestador | Puede publicar |
|---|---|
| ALOJAMIENTO | ALOJAMIENTO |
| RESTAURANTE | RESTAURANTE |
| AGENCIA_OPERADORA | ACTIVIDAD, PAQUETE |
| GUIA_INDEPENDIENTE | ACTIVIDAD, PAQUETE |

- RF-65: "las ofertas de su tipo y, si es agencia operadora o guía independiente, paquetes de dos o más de sus actividades".
- RF-67: las actividades del **guía independiente** exigen punto de encuentro, duración, modalidad e idiomas. Para las agencias son opcionales.
- **Diferida (RF-69):** la modalidad de una actividad del guía debe estar entre las modalidades que declaró. Esa lista vive en el núcleo; si se aplica, la réplica de DC-02 tendrá que incluirla.

**DC-11. Publicar y editar exigen el RNT activo.** Sin él se responde 403 con el código `RNT_INACTIVO`, para que el frontend le explique al prestador qué pasó. Aplica también a editar, porque una edición crea una versión nueva para moderar.

**DC-12. La zona debe existir y estar activa** (RF-84). Si no, se responde 400 con el código `ZONA_NO_DISPONIBLE`.

## API

**DC-13. Rutas del prestador en `/prestador/**`.** Así se puede autorizar por ruta (el rol PRESTADOR) en un solo lugar.

| Controlador | Endpoint |
|---|---|
| `OfertaPrestadorController` | `POST /prestador/ofertas`, `PUT /prestador/ofertas/{id}`, `PATCH /prestador/ofertas/{id}/retiro`, `GET /prestador/ofertas` |
| `OfertaPublicaController` | `GET /ofertas?tipo&zonaId&precioMin&precioMax&tipoActividad&nivelEsfuerzoMaximo&accesibleSillaRuedas&page&size`, `GET /ofertas/{id}` |
| `ZonaController` | `GET /zonas` |
| `AvisoInternoController` | `POST /internal/avisos` |

- En el prototipo todos los casos de uso tienen endpoint; el contrato está en `contratos/catalogo.openapi.yaml`. Retirar no lleva cuerpo y responde 204; retirar una oferta ya retirada no cambia nada y también responde 204. Listar las ofertas propias usa la paginación de DC-14 y devuelve la oferta como la ve su prestador (`OfertaPrestador`), incluidas las retiradas.
- En el prototipo no se verifica el rol: `ResolutorIdentidad`, en `compartido/seguridad`, solo resuelve quién es el prestador (DC-01). La autorización por rol pasa a Spring Security cuando llegue `libs/seguridad-jwt`, sin tocar los controladores.

**DC-14. Paginación.** `page` empieza en 0, como en Spring Data; `size` vale 20 por defecto y como máximo 100. El tope protege la latencia de QAS-07. Un `page` negativo o un `size` fuera de 1 a 100 responde 400. El orden es estable para que no se repitan ni se salten resultados entre páginas: la búsqueda pública ordena por precio y luego por id; las ofertas del prestador, por id.

**DC-15. Los errores salen en formato ProblemDetail (RFC 7807)**, con una propiedad `codigo` estable:

| Código | HTTP | Cuándo |
|---|---|---|
| `DATO_INVALIDO` | 400 | Un campo no cumple las reglas |
| `ZONA_NO_DISPONIBLE` | 400 | La zona no existe o está inactiva |
| `PRESTADOR_NO_AUTORIZADO` | 403 | Quien edita o retira no es el dueño |
| `TIPO_OFERTA_NO_PERMITIDO` | 403 | DC-10 |
| `RNT_INACTIVO` | 403 | DC-11 |
| `OFERTA_NO_ENCONTRADA` | 404 | No existe, o no es visible para quien la consulta |
| `OFERTA_NO_EDITABLE` | 409 | La oferta está retirada |
| `NO_AUTENTICADO` | 401 | Falta la identidad del prestador, o llegan los headers de demostración sin el perfil `demo` (DC-01) |
| `CONFLICTO_CONCURRENCIA` | 409 | Otra operación modificó la misma oferta o la misma habilitación al mismo tiempo y los reintentos no alcanzaron (ADR-05) |

Un JSON mal formado o un parámetro con el formato equivocado, por ejemplo un precio que no es un número, también responde 400 `DATO_INVALIDO`.

## Persistencia (MongoDB)

**DC-16. Modelo documental de `ofertas`.** Una sola colección `ofertas`, con un documento por oferta: `{_id, tipo, prestadorId, retirada, rntPrestadorActivo, publicada: {...}, pendiente: {...}}`.
- El discriminador `tipo` va en la raíz, porque no cambia, y cada versión guarda solo los campos de su tipo.
- El precio se guarda como `Decimal128`. Por defecto Spring lo guardaría como texto y la búsqueda por rango de precio fallaría.
- Se desactiva el campo `_class` de Spring.
- Otras colecciones: `zonas`, `prestadores` (réplica de DC-02) y `avisos_procesados` (`_id = idAviso`).

**DC-17. Índice parcial para la búsqueda (evidencia de QAS-07).**

```
{ tipo: 1, "publicada.zonaId": 1, "publicada.precioReferencia": 1, _id: 1 }
partialFilterExpression: { rntPrestadorActivo: true, retirada: false, publicada: { $exists: true } }
```

- El índice solo contiene ofertas visibles, así que es pequeño, y su clave coincide con la documentada en QAS-07. `_id` al final da el orden estable de DC-14.
- La prueba de integración verifica con `explain()` que la búsqueda usa el índice (`IXSCAN`, no `COLLSCAN`).
- Limitación aceptada: si la búsqueda no filtra por `tipo`, el índice se usa igual, pero recorre más claves.
- Aparte va `{ prestadorId: 1 }`, para el `updateMany` de DC-03 y para listar las ofertas del prestador.

## Datos

**DC-18. Zonas de la precarga** (D-04, [SUPUESTO] hasta validarlas con una fuente oficial). Las coordenadas son aproximadas, del centro de cada zona.

| Zona | Marcas |
|---|---|
| Centro Histórico | ninguna |
| El Rodadero y Gaira | sin definir |
| Taganga | alta afluencia |
| Zona sur hotelera (Pozos Colorados, Bello Horizonte, Playa Salguero) | sin definir |
| Parque Tayrona | área protegida + alta afluencia |
| Troncal del Caribe (Guachaca, Buritaca, Don Diego) | sin definir |
| Minca y Sierra Nevada (Bonda, Minca, Ciudad Perdida) | área protegida |

"Ecosistema sensible" no es un campo aparte: RF-84 lo une con área protegida en la marca `areaProtegida`.

**DC-19. Validaciones de campo provisionales.** Todas viven en `dominio/modelo/Validaciones` y se completan cuando se cierre el SRS. Hoy se aplican:
- Obligatorios: nombre, descripción, precio de referencia, zona, capacidad máxima; en la actividad, tipo de actividad, nivel de esfuerzo y al menos un horario de salida (el turista elige uno al enviar la solicitud, RF-30).
- Precio no negativo; capacidad y duración mayores que cero.
- Idiomas con códigos de dos letras ("es", "en").
- Opcionales: horarios, ubicación, cómo llegar y tipo de unidad del alojamiento.
- Faltan del SRS: longitudes máximas, rango de precio y cualquier otro obligatorio.

**DC-20. Stack del prototipo.** JDK 21 (no Java 25) con Spring Boot 3.5.x. La capa `aplicacion` puede usar `@Service` y `@Transactional`; lo que debe quedar libre de frameworks es el `dominio`, y lo verifica ArchUnit en cada build.

**DC-21. Un servicio de aplicación por caso de uso.** Cada puerto de entrada tiene su propia implementación: `GestionarOfertaService`, `BuscarOfertasService`, `ConsultarFichaOfertaService`, `ConsultarZonasService` y `AplicarAvisoNucleoService`. El diagrama de paquetes lo permite literalmente ("cada caso de uso puede implementarse en su propia clase; el diagrama los agrupa en un servicio por módulo"). Se prefirió a un único `CatalogoService` porque ese servicio atendía a tres actores (prestador, turista y núcleo) con dependencias casi disjuntas: separarlo cumple el principio de responsabilidad única y hace que un caso de uso nuevo sea una clase nueva, sin modificar las existentes. La regla "un prestador sin avisos no está habilitado" quedó en un solo lugar, `PrestadorCatalogoRepositoryPort.habilitacionDe`.

## Paquetes, búsqueda y perfil público

**DC-22. Unidad del precio de referencia por tipo de oferta.**

| Tipo de oferta | Unidad |
|---|---|
| ACTIVIDAD | por persona |
| PAQUETE | por persona |
| ALOJAMIENTO | por noche y unidad |
| RESTAURANTE | promedio por persona |

El paquete se cobra por persona porque está hecho de actividades, que se cobran por persona.

**DC-23. Actividades que puede incluir un paquete (`ReglasPaquete`).**
- Dos o más, distintas, todas de tipo ACTIVIDAD y del mismo prestador (RF-22).
- Una actividad **retirada** no se puede incluir: el paquete se rechaza al publicarlo o editarlo, con 400 `DATO_INVALIDO` (DC-15). El retiro es definitivo (DC-09), así que no tiene sentido incluirla.
- Una actividad **sin aprobar**, en moderación o rechazada, sí se puede incluir: el prestador puede enviar a moderación las actividades y el paquete al mismo tiempo. El paquete no es visible hasta que todas sus actividades lo sean (RF-22).
- Una referencia a una oferta que no existe, que no es una actividad o que es de otro prestador responde el mismo 400 `DATO_INVALIDO`, sin distinguir el caso, para no revelar ofertas ajenas.

**DC-24. El filtro por tipo de actividad incluye los paquetes.** Un paquete coincide si al menos una de sus actividades es de ese tipo. Sus `tiposActividad` (conjunto) se derivan de las versiones publicadas de sus actividades, igual que su nivel de esfuerzo y su accesibilidad en silla de ruedas (`ReglasPaquete`). El filtro busca en `tipoActividad` para las actividades y en `tiposActividad` para los paquetes.
- Los valores no se guardan: se calculan al consultar con `ReglasPaquete`, a partir de las actividades ya cargadas.
- Pendiente: guardar `tiposActividad`, `nivelEsfuerzo`, `accesibleSillaRuedas` y `actividadesVigentes` en el paquete se decide al construir el adaptador de MongoDB, y solo si la búsqueda lo necesita para usar índices (QAS-07).

**DC-25. El filtro por nivel de esfuerzo es "hasta el nivel indicado".** Incluye los niveles menores:

| Filtro | Devuelve |
|---|---|
| BAJO | BAJO |
| MODERADO | BAJO y MODERADO |
| ALTA_EXIGENCIA | todos |

- El filtro existe para personas con limitaciones físicas (RF-17), que buscan lo que pueden hacer y no un nivel exacto.
- Para un paquete se compara con su nivel calculado: el mayor de sus actividades.
- El orden de exigencia está definido de forma explícita en `NivelEsfuerzo`; no depende del orden en que se declaran los valores.
- Los alojamientos y los restaurantes no tienen nivel de esfuerzo, así que no aparecen cuando se usa este filtro.

**DC-26. Perfil público: solo la descripción es obligatoria.** Las fotos, los horarios de atención, el sitio web y las redes sociales son opcionales, porque el proyecto exige incluir a los prestadores pequeños con pocos recursos tecnológicos (RS-03).
- Si vienen, el sitio web, las redes sociales y las fotos deben ser URLs `http` o `https`; si no, se responde 400 `DATO_INVALIDO`.
- No hay límite de cantidad de fotos: no está definido y queda como `TODO` en `PerfilPublico`.

## Prototipo de la API

**DC-27. Perfiles de ejecución.**

| Perfil | Qué activa | Cuándo |
|---|---|---|
| `memoria` | Los adaptadores de salida en la memoria del proceso (`infraestructura/salida/memoria`), con los datos de demostración de Santa Marta | Por defecto, mientras no exista el adaptador de MongoDB |
| `demo` | Los headers que sustituyen al token del prestador (DC-01) | Nunca por defecto; solo en la demostración |

- Docker Compose activa los dos (`SPRING_PROFILES_ACTIVE=memoria,demo`).
- Con `memoria`, la salud del servicio no depende de MongoDB.
- De Actuator solo se expone `/actuator/health`, para el healthcheck de Docker Compose; Nginx no lo publica hacia afuera.

**DC-28. Con los datos en memoria, cada réplica tiene su propio estado.** Lo que se escribe en una réplica no lo ve la otra.
- Todas las réplicas arrancan con los mismos datos, con identificadores fijos, así que la búsqueda y la ficha se demuestran con 2 réplicas y el header `X-Instancia` alternando.
- El flujo de escritura (publicar, aprobar con un aviso y ver la oferta en la búsqueda) se demuestra con 1 réplica.
- La limitación desaparece con el adaptador de MongoDB, donde el estado es compartido. El control de concurrencia de esas escrituras está en el ADR-05.

**DC-29. Forma de las respuestas.**
- La búsqueda devuelve un resumen de cada oferta: id, tipo, nombre, descripción, precio de referencia, unidad del precio y zona.
- La ficha devuelve el contenido completo, la zona con sus buenas prácticas y condiciones de visita (RF-25), el perfil público del prestador si lo completó (RF-24) y un detalle según el tipo: `actividad`, `paquete` o `alojamiento`.
- En la ficha de un paquete, el nivel de esfuerzo, los tipos de actividad y la accesibilidad se calculan con sus actividades (DC-24).
- Publicar y editar devuelven la oferta como la ve su prestador: la versión publicada y la pendiente, cada una con su número y su estado de moderación.

## Modelo documental en MongoDB

**DC-30. Los identificadores del catálogo son `ObjectId`.** Siguen el modelo documental: `_id` de `ofertas`, `zonas`, `perfiles` y `opiniones`, y las referencias `zonaId`, `actividadIds` y `opiniones.ofertaId`.
- El id de una oferta nueva lo genera la persistencia con `OfertaRepositoryPort.nuevoId()`, no el servicio de aplicación. En MongoDB es un `ObjectId`; para el dominio sigue siendo un texto opaco.
- El adaptador en memoria genera el mismo formato, y los datos de demostración usan ids fijos con formato `ObjectId` (zonas `a0…`, ofertas `b0…`). Así la demostración y el README sirven igual con los dos perfiles.
- Un id que no tiene formato `ObjectId` se trata como inexistente: 404 en la ficha y `ZONA_NO_DISPONIBLE` o `DATO_INVALIDO` en las referencias.
- Los ids de prestadores y turistas siguen siendo `UUID`, porque los emite el núcleo.

**DC-31. La búsqueda de paquetes usa una agregación.** La visibilidad de un paquete (RF-22) y los filtros por tipo de actividad, nivel de esfuerzo y accesibilidad (DC-24, DC-25) dependen de sus actividades, que son otros documentos.
- La búsqueda es una agregación: `$match` sobre el índice de QAS-07 (DC-17), `$lookup` de las actividades de los paquetes, el filtro sobre los valores calculados y `$facet` para la página y el total. La paginación y el total quedan correctos.
- Los valores del paquete no se guardan; se calculan al consultar, como en el adaptador en memoria.
- Si las pruebas de carga de la iteración 11 lo exigen, se pasa a guardar los valores calculados en el paquete y a mantenerlos al aprobar, editar o retirar sus actividades.

**DC-32. El servicio de recomendación no tiene base de datos.** Se corrige el modelo documental, que mostraba una base `recomendacion` con `registro_exposicion` y `cache_alertas`. Según el ADR-02, el ADR-03 y el diagrama de paquetes, las alertas del IDEAM viven en una caché en memoria del servicio y el registro de exposición se envía al núcleo, que lo guarda en PostgreSQL (RET-02).

## Cambios pendientes en los documentos de diseño

1. **Vista general de paquetes, interacción 2:** quitar "tipo de prestador" de los avisos del núcleo al catálogo (DC-01).
2. **Diagrama de paquetes, estructura del repositorio:** cambiar "Java 25" por "Java 21" (DC-20).
3. **Diagrama de clases y diccionario:**
   - agregar `PrestadorCatalogo` como clase de réplica del catálogo (DC-02);
   - agregar `rntPrestadorActivo` en `Oferta` (DC-03) y `numero` en `VersionOferta` (DC-05);
   - cambiar `duracionMinutos : int` por `Integer` en `VersionActividad`, y dejar `modalidad` y `puntoEncuentro` como opcionales: solo son obligatorios para el guía (DC-10);
   - quitar `esAccesibleSillaRuedas()` de los métodos abstractos de `VersionOferta` y `nivelEsfuerzo()` de `VersionPaquete`: actividad, alojamiento y restaurante guardan su accesibilidad, y los valores del paquete se calculan con sus actividades en `ReglasPaquete` (DC-24);
   - agregar `actividadIds` en `VersionPaquete`: el paquete referencia sus actividades por el identificador de su oferta;
   - agregar `prestadorId` en `PerfilPublico` (DC-26).
4. **Modelo documental (MongoDB)** (sección 2 del documento de diseño):
   - quitar la base `recomendacion` (DC-32);
   - marcar `publicada` como `| null`: una oferta nueva solo tiene versión pendiente (DC-06);
   - marcar `ubicacion` y `tipoUnidad` como opcionales mientras el SRS no diga lo contrario (DC-19);
   - agregar los índices: el parcial de QAS-07 y `{prestadorId}` en `ofertas` (DC-17), único en `perfiles.prestadorId` y único en `{ofertaId, turistaId}` de `opiniones` (RF-61);
   - usar el mismo nombre de base que el diagrama de despliegue y Docker Compose: `catalogo_db`.
5. **Diagrama del módulo catálogo:** agregar el puerto de salida `PrestadorCatalogoRepositoryPort` (réplica de DC-02) con su adaptador `PrestadorMongoAdapter`.
6. **Agrupaciones técnicas del código** que no cambian el modelo, por si el docente compara: `ContenidoComun` agrupa los atributos comunes de `VersionOferta`, y `RegistroModeracion` agrupa número, estado, motivo y fechas de moderación de cada versión.

## Fuera del alcance del prototipo

Opiniones; la gestión del perfil público por el prestador (RF-64); gestión de zonas; imágenes (MinIO); JWT real; autenticación entre servicios; la regla de modalidades del guía (RF-69).
