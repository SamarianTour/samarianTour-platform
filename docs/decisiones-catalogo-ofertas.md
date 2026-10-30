# Decisiones del prototipo: ofertas del catálogo

Registro de las decisiones tomadas para el prototipo del servicio `catalogo` (gestión de ofertas del prestador, tipos ACTIVIDAD, PAQUETE, ALOJAMIENTO y RESTAURANTE). Complementa el diagrama de paquetes, el diagrama de clases y el diccionario de clases; donde los contradice, se indica en la sección final.

- **Estado:** vigente, octubre de 2026. Pendiente de rectificar contra el SRS cuando termine su revisión.
- **Rama:** `feature/catalogo-ofertas`.
- **Convención:** ✅ decisión que sale de un requisito o de una decisión anterior (se cita la fuente). 🆕 decisión nueva del equipo.

## Resumen

| # | Decisión | Origen |
|---|---|---|
| DC-01 | La identidad viene del token de acceso del núcleo, validado aquí con su clave pública | ✅ reparto Núcleo/Catálogo, DN-37 |
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
| DC-13 | Autorización por prefijo de ruta: `/prestador/**`, `/admin/**` y `/turista/**` | 🆕 |
| DC-14 | Paginación | 🆕 QAS-07 |
| DC-15 | Errores en ProblemDetail con código de motivo | 🆕 |
| DC-16 | Modelo documental de `ofertas` | ✅ ADR-03 |
| DC-17 | Índices parciales para la búsqueda | 🆕 QAS-07 |
| DC-18 | Zonas de la precarga | ✅ D-04 [SUPUESTO] |
| DC-19 | Validaciones de campo provisionales | 🆕 hasta cerrar el SRS |
| DC-20 | Stack del prototipo | 🆕 |
| DC-21 | Un servicio de aplicación por caso de uso | ✅ diagrama de paquetes |
| DC-22 | Unidad del precio de referencia por tipo de oferta | 🆕 RF-20 |
| DC-23 | Actividades que puede incluir un paquete | 🆕 RF-22, DC-09 |
| DC-24 | El filtro por tipo de actividad incluye los paquetes | 🆕 |
| DC-25 | El filtro por nivel de esfuerzo es "hasta el nivel indicado" | 🆕 RF-17 |
| DC-26 | Perfil público: solo la descripción es obligatoria | 🆕 RF-64, RS-03 |
| DC-27 | Perfiles de ejecución `memoria`, `mongo` y `demo` | 🆕 |
| DC-28 | Con MongoDB las réplicas comparten el estado; con los datos en memoria, no | 🆕 QAS-02 |
| DC-29 | Forma de las respuestas de la API | 🆕 RF-20, RF-24, RF-25 |
| DC-30 | Los identificadores del catálogo son `ObjectId` | ✅ modelo documental |
| DC-31 | La búsqueda de paquetes usa una agregación | 🆕 RF-22, QAS-07 |
| DC-32 | El servicio de recomendación no tiene base de datos | ✅ ADR-02, ADR-03 |
| DC-33 | Implementación de los adaptadores de MongoDB | 🆕 ADR-05, DC-16, DC-17 |
| DC-34 | El sello RNT se muestra sin el número de registro | 🆕 desvía RF-24 |
| DC-35 | Consulta interna de una oferta para el núcleo | ✅ RF-30, modelo relacional |
| DC-36 | Ofertas visibles para la recomendación, sin datos que sesguen | ✅ RF-26, RF-29, RET-02 |
| DC-37 | Búsqueda por texto con expresiones regulares, sin índice de texto | 🆕 RF-14, QAS-07 |
| DC-38 | El catálogo lista lo que espera moderación; el núcleo decide | ✅ RF-79, diagrama del núcleo |
| DC-39 | Perfil gestionado por el prestador y fotos reprocesadas en MinIO | ✅ RF-64, RF-72, diagrama del módulo |
| DC-40 | El módulo `opiniones` consulta las ofertas solo por `catalogo.api` | ✅ reglas de arquitectura |
| DC-41 | La elegibilidad para opinar llega del núcleo por aviso | 🆕 RF-60 [SUPUESTO] |
| DC-42 | Validaciones y estados de la opinión | 🆕 RF-61 [SUPUESTO] |
| DC-43 | Lectura pública de las opiniones | ✅ RF-59 |
| DC-44 | Moderación de opiniones: el catálogo lista, el núcleo decide | ✅ RF-80, DC-38 |
| DC-45 | Supresión de las opiniones de un turista | ✅ Ley 1581, bandeja de salida |
| DC-46 | Persistencia de opiniones | 🆕 RF-61, DC-30, DC-33 |
| DC-47 | Las ofertas llevan de 1 a 6 fotos moderadas; las que nadie usa se borran | 🆕 RF-65, RF-66 |
| DC-48 | Evidencia de QAS-07: métricas en el servidor y prueba de carga de la búsqueda | ✅ QAS-07, QAS-02; RNF-13 en parte |
| DC-49 | El administrador registra, edita, marca e inactiva las zonas; nunca se eliminan | ✅ HU-ADM-06, RF-84, RF-25 |
| DC-50 | El prestador consulta su oferta completa, también la que nunca se publicó | 🆕 RF-65, RF-66, RF-79 |

## Identidad y habilitación del prestador

**DC-01. La identidad viene del token.** El catálogo autoriza con el id del usuario, su rol y, si es prestador, su tipo, tomados del token de acceso que emite el núcleo (`Authorization: Bearer`). Lo valida aquí mismo, con la clave pública del núcleo (`SEGURIDAD_JWT_CLAVE_PUBLICA`) y `libs/seguridad-jwt`, sin consultarlo (DN-37). El tipo de prestador **no** se guarda en el catálogo, para no tener dos fuentes de verdad.
- El prestador que todavía no registró su RNT en el núcleo no trae tipo en el token: puede completar su perfil y subir fotos, pero las rutas de `/prestador/ofertas` responden 403 `ACCESO_DENEGADO` hasta que lo registre y renueve la sesión.
- En el prototipo la identidad llegaba en los headers `X-Usuario-Id`, `X-Rol` y `X-Tipo-Prestador`, solo con el perfil `demo`. Desde DN-37 ya no identifican a nadie.
- Cualquiera puede enviar esos headers, así que solo se aceptan con el perfil `demo` (DC-27). Sin ese perfil, las rutas que exigen identidad responden 401 `NO_AUTENTICADO`.
- Roles que el catálogo distingue: `TURISTA`, `PRESTADOR` y `ADMINISTRADOR`. El tipo de prestador solo se exige con el rol `PRESTADOR`.

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
- Nginx responde 404 a `/api/catalogo/internal/**` desde afuera, y 400 a una ruta con `;`, que antes esquivaba ese bloqueo (DN-37). Dentro de la red de Docker, cada ruta interna exige además el token del servicio que la usa: el núcleo o la recomendación (DN-38).

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

**DC-13. Autorización por prefijo de ruta.** Cada prefijo exige un rol y se verifica en un solo lugar (`compartido/config/SeguridadConfig`, con Spring Security, DN-37) antes de llegar al controlador: `/prestador/**` PRESTADOR, `/admin/**` ADMINISTRADOR y `/turista/**` TURISTA. Sin identidad, 401 `NO_AUTENTICADO`; con otro rol, 403 `ACCESO_DENEGADO`. La búsqueda, la ficha y las zonas son públicas, y `/internal/**` solo es accesible desde la red interna. El controlador lee la identidad ya verificada con `Identidad.de(peticion)`.

| Controlador | Endpoint |
|---|---|
| `OfertaPrestadorController` | `POST /prestador/ofertas`, `PUT /prestador/ofertas/{id}`, `PATCH /prestador/ofertas/{id}/retiro`, `GET /prestador/ofertas`, `GET /prestador/ofertas/{id}` |
| `OfertaPublicaController` | `GET /ofertas?texto&tipo&zonaId&precioMin&precioMax&tipoActividad&nivelEsfuerzoMaximo&accesibleSillaRuedas&page&size`, `GET /ofertas/{id}` |
| `ZonaController` | `GET /zonas`, `GET /zonas/{zonaId}` |
| `ZonaAdminController` | `GET /admin/zonas`, `POST /admin/zonas`, `PUT /admin/zonas/{zonaId}` |
| `AvisoInternoController` | `POST /internal/avisos` |
| `ConsultaInternaController` | `GET /internal/ofertas`, `GET /internal/ofertas/{id}` |
| `ModeracionController` | `GET /admin/moderacion/ofertas`, `GET /internal/moderacion/ofertas/{id}` |
| `OpinionPublicaController` (módulo opiniones) | `GET /ofertas/{id}/opiniones` |
| `OpinionTuristaController` (módulo opiniones) | `POST /turista/opiniones`, `GET /turista/opiniones` |
| `ModeracionOpinionesController` (módulo opiniones) | `GET /admin/moderacion/opiniones`, `GET /internal/moderacion/opiniones/{id}` |
| `AvisoOpinionesController` (módulo opiniones) | `POST /internal/opiniones/avisos` |

- En el prototipo todos los casos de uso tienen endpoint; el contrato está en `contratos/catalogo.openapi.yaml`. Retirar no lleva cuerpo y responde 204; retirar una oferta ya retirada no cambia nada y también responde 204. Listar las ofertas propias usa la paginación de DC-14 y devuelve la oferta como la ve su prestador (`OfertaPrestador`), incluidas las retiradas.
- Con `libs/seguridad-jwt` la identidad sale del token y `ExigirRol` pasó a Spring Security sin tocar los controladores, que solo leen `Identidad.de(peticion)` (DN-37).

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
| `ZONA_NO_ENCONTRADA` | 404 | La zona no existe (DC-49) |
| `ZONA_DUPLICADA` | 409 | Ya hay otra zona con ese nombre (DC-49) |
| `NO_AUTENTICADO` | 401 | Falta la identidad del usuario, o llegan los headers de demostración sin el perfil `demo` (DC-01) |
| `ACCESO_DENEGADO` | 403 | El rol del usuario no es el que exige el prefijo de la ruta (DC-13) |
| `OPINION_NO_HABILITADA` | 403 | El turista no tiene una solicitud pagada de la oferta (DC-41) |
| `OPINION_DUPLICADA` | 409 | El turista ya opinó sobre la oferta (RF-61) |
| `OPINION_NO_ENCONTRADA` | 404 | La opinión no existe o ya no está en moderación (DC-44) |
| `CONFLICTO_CONCURRENCIA` | 409 | Otra operación modificó la misma oferta, habilitación u opinión al mismo tiempo y los reintentos no alcanzaron (ADR-05) |

Un JSON mal formado o un parámetro con el formato equivocado, por ejemplo un precio que no es un número, también responde 400 `DATO_INVALIDO`.

## Persistencia (MongoDB)

**DC-16. Modelo documental de `ofertas`.** Una sola colección `ofertas`, con un documento por oferta: `{_id, tipo, prestadorId, retirada, rntPrestadorActivo, publicada: {...}, pendiente: {...}}`.
- El discriminador `tipo` va en la raíz, porque no cambia, y cada versión guarda solo los campos de su tipo.
- El precio se guarda como `Decimal128`. Por defecto Spring lo guardaría como texto y la búsqueda por rango de precio fallaría.
- Se desactiva el campo `_class` de Spring.
- Otras colecciones: `zonas` (con `nombreClave` y su índice único, DC-49), `prestadores` (réplica de DC-02), `perfiles` (un perfil público por prestador, con `nombreComercial` opcional, DC-26) y `avisos_procesados` (`_id = idAviso`). Las del módulo de opiniones están en DC-46.

**DC-17. Índices parciales para la búsqueda (evidencia de QAS-07).**

```
busqueda_tipo_zona_precio: { tipo: 1, "publicada.zonaId": 1, "publicada.precioReferencia": 1, _id: 1 }
busqueda_precio:           { "publicada.precioReferencia": 1, _id: 1 }
partialFilterExpression (los dos): { rntPrestadorActivo: true, retirada: false, publicada: { $exists: true } }
```

- Los dos índices solo contienen ofertas visibles, así que son pequeños. La clave del primero coincide con la documentada en QAS-07; `_id` al final da el orden estable de DC-14.
- El segundo cubre la búsqueda que no filtra por tipo: la recorre ya ordenada por precio, sin ordenar en memoria.
- MongoDB solo usa un índice parcial si la consulta incluye las condiciones de su filtro; la búsqueda siempre las incluye, porque son las de `ReglaVisibilidad`.
- Las pruebas de integración verifican con `explain()` que la búsqueda usa un índice (`IXSCAN`, no `COLLSCAN`), con filtros y sin ellos.
- Aparte van `ofertas_del_prestador`, `{ prestadorId: 1, _id: 1 }`, para el `updateMany` de DC-03 y para listar las ofertas del prestador en orden de id, y `perfil_por_prestador`, único sobre `perfiles.prestadorId`.

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
- En MongoDB tampoco se guardan: la búsqueda los calcula en la agregación de DC-31.

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

**DC-26. Perfil público: solo la descripción es obligatoria.** El nombre comercial, las fotos, los horarios de atención, el sitio web y las redes sociales son opcionales, porque el proyecto exige incluir a los prestadores pequeños con pocos recursos tecnológicos (RS-03).
- Si vienen, el sitio web y las redes sociales deben ser URLs `http` o `https`; si no, se responde 400 `DATO_INVALIDO`.
- Las fotos se suben al almacenamiento de fotos, como máximo 6 (DC-39).

## Prototipo de la API

**DC-27. Perfiles de ejecución.**

| Perfil | Qué activa | Cuándo |
|---|---|---|
| `memoria` | Los adaptadores de salida en la memoria del proceso (`infraestructura/salida/memoria`), con los datos de demostración de Santa Marta | Por defecto, y en las pruebas que no necesitan MongoDB |
| `mongo` | Los adaptadores de salida de MongoDB (`infraestructura/salida/persistencia`) y la creación de sus índices (DC-33) | En Docker Compose y en las pruebas de integración |
| `demo` | Junto con `mongo`, la carga de los datos de demostración | Nunca por defecto; solo en la demostración |
| `carga` | Junto con `mongo`, el catálogo sintético de 5.000 ofertas después de los datos de demostración (DC-48) | Solo en la prueba de carga |

- Docker Compose activa `mongo` y `demo` (`SPRING_PROFILES_ACTIVE=mongo,demo`).
- **Carga de los datos de demostración** (`CargaDemoMongo`, [SUPUESTO] perfiles `mongo` y `demo` a la vez): al arrancar inserta en MongoDB los mismos datos de Santa Marta que usa el perfil `memoria`. Una colección solo se carga si está vacía, así que no toca una base que ya tiene datos.
- Las dos réplicas arrancan a la vez y pueden ver vacía la misma colección: cada documento se inserta por separado, con su identificador fijo, y un duplicado se ignora. La carga queda idempotente sin coordinación entre réplicas.
- Con `memoria`, la salud del servicio no depende de MongoDB; con `mongo`, sí.
- De Actuator solo se expone `/actuator/health`, para el healthcheck de Docker Compose; Nginx no lo publica hacia afuera.

**DC-28. Con MongoDB las réplicas comparten el estado; con los datos en memoria, no.**
- Con el perfil `mongo`, que es el de Docker Compose, lo que se escribe por una réplica lo ve la otra. Toda la demostración, lectura y escritura, se hace con las 2 réplicas y el header `X-Instancia` muestra cuál respondió.
- Los avisos procesados también están en la base, así que un aviso repetido se reconoce aunque llegue a otra réplica.
- El control de concurrencia de esas escrituras está en el ADR-05.
- Con el perfil `memoria` cada réplica tiene su propio estado: sirve para una sola instancia y para las pruebas sin MongoDB, no para un despliegue con réplicas.

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

**DC-33. Implementación de los adaptadores de MongoDB.** Los cinco adaptadores del diagrama del módulo (`OfertaMongoAdapter`, `ZonaMongoAdapter`, `PerfilMongoAdapter`, `PrestadorMongoAdapter` y `AvisoProcesadoMongoAdapter`) viven en `infraestructura/salida/persistencia`, con los documentos en `document/` y los mappers en `mapper/`.
- **`MongoTemplate`, sin repositorios de Spring Data.** El reemplazo condicionado por revisión, el upsert filtrado por versión y el `updateMany` del ADR-05, y la agregación de DC-31, no se expresan con un repositorio derivado. Usar repositorios solo en zonas y perfiles mezclaría dos estilos.
- **Revisión explícita, sin `@Version`.** Una oferta nueva (revisión 0) se inserta con la revisión 1; una oferta leída se reemplaza con el filtro `{_id, revision}` y queda con la siguiente; si el filtro no encuentra nada, el adaptador lanza `ConflictoConcurrenciaException`. Con `@Version`, Spring trata la revisión 0 como entidad nueva y la guarda como 0, así que el mapper tendría que correr la revisión en uno. El adaptador en memoria hace lo mismo y también guarda sus datos iniciales con la revisión 1.
- **Réplica de habilitación:** un upsert con el filtro `{_id, version < nueva}`. Si la réplica guardada tiene una versión igual o mayor, el upsert intenta crear otra con el mismo `_id`, la base lo rechaza y el adaptador lanza el conflicto.
- **Avisos procesados:** el `_id` es el idAviso, guardado siempre como texto; un `_id` repetido se ignora.
- **Formato:** los documentos son records con los campos del modelo documental; los campos sin valor no se guardan. El precio es `Decimal128`, las referencias son `ObjectId`, los UUID usan la representación estándar (subtipo 4), las ubicaciones son puntos GeoJSON `[longitud, latitud]` y las horas de salida son textos `HH:mm`. `MongoConfig` (en `compartido/config`) quita el campo `_class`. MongoDB guarda las fechas con precisión de milisegundos.
- **Índices:** los crea `IndicesMongo` al arrancar con el perfil `mongo`; crear un índice que ya existe no hace nada, así que todas las réplicas pueden hacerlo. Así también los tienen las pruebas de integración.
- **Pruebas:** `ContratoOfertaRepository` y `ContratoPrestadorRepository` son las mismas pruebas para los dos adaptadores, en memoria y en MongoDB, con los datos de demostración. Los datos de demostración pasaron a `infraestructura/salida/demo` para compartirlos.

**DC-34. El sello RNT se muestra sin el número de registro.** RF-24 pide mostrar el sello "RNT verificado" con el número de registro. El equipo decidió mostrar solo el sello. El número es un dato del núcleo, que lo verifica contra la API del RNT, y el catálogo solo recibe si la habilitación está activa (DC-02); mostrarlo obligaría a replicar un dato más en los avisos de habilitación. Queda registrado como desviación de RF-24 para ajustar el SRS.

## Consultas internas

**DC-35. Consulta interna de una oferta para el núcleo.** `GET /api/catalogo/internal/ofertas/{id}` (`ConsultaInternaController`, `ConsultarOfertaInternaUseCase`, RF-30). El núcleo la llama al crear una solicitud, con el corte de circuito de 3 s del ADR-04.
- Devuelve lo que la tabla `solicitud` copia de la oferta (`nombre`, `tipo`, `zonaId` y `prestadorId`) y lo que el núcleo valida: `horariosSalida`, para la hora que elige el turista (solo actividad y paquete), y `capacidadMaxima`, para el número de personas. Todo sale de la versión publicada.
- No devuelve el tipo de actividad: el tablero de demanda agrupa por zona y tipo de oferta (CU-09 del Entregable 2), y la solicitud no lo guarda. Tampoco el precio: el precio base lo registra el operador al atender la solicitud (RF-37) y es el que se cobra (RF-46, RF-49); el precio de referencia del catálogo no es vinculante (RF-20).
- Aplica la misma visibilidad que la ficha: si la oferta no existe, no tiene versión aprobada, está retirada, su prestador tiene el RNT inactivo o es un paquete con alguna actividad no visible, responde 404 `OFERTA_NO_ENCONTRADA` y la solicitud no se crea.

**DC-36. Ofertas visibles para la recomendación.** `GET /api/catalogo/internal/ofertas` (`ConsultarOfertasVisiblesUseCase`, RF-26, RF-29, RET-02). Es la base de conocimiento cerrada de la recomendación, que la guarda en caché.
- Reutiliza la búsqueda pública: misma visibilidad, mismo orden (precio de referencia y luego id) y misma paginación (hasta 100 por página), con filtros opcionales `tipo` y `zonaId`.
- Cada oferta trae los datos de los criterios explícitos de la recomendación (`ReglaCriteriosExplicitos`): tipo de oferta, tipos de actividad, zona, accesibilidad, nivel de esfuerzo y precio; el nombre y la descripción, para explicar la recomendación (RF-27); y `zonaAltaAfluencia`, para proponer alternativas fuera de las zonas congestionadas (RF-29, `ReglaAlternativas`). En un paquete, los tipos de actividad, el esfuerzo y la accesibilidad se calculan con sus actividades, como en la búsqueda (DC-24, DC-25).
- `prestadorId` va para que la recomendación arme el registro de exposición que mide el sesgo por prestador (RET-02).
- **No** van calificaciones, opiniones, comisión ni el tamaño del prestador: así la recomendación no puede favorecer a nadie por esos datos (RET-02). El catálogo no prioriza a ningún prestador; la rotación entre ofertas de igual afinidad es de la recomendación (`PoliticaRotacion`).

Las dos rutas no exigen rol de usuario: Nginx bloquea `/api/catalogo/internal/` hacia afuera, como en DC-04, y adentro piden el token de servicio de quien las usa (DN-38).

## Búsqueda por texto

**DC-37. Búsqueda por texto sin índice de texto.** `GET /api/catalogo/ofertas?texto=...` (RF-14), combinable con los demás filtros.
- **Reglas:** cada palabra del texto debe aparecer en el nombre o en la descripción de la versión publicada, aunque sea incompleta ("buce" encuentra "buceo"), sin distinguir mayúsculas ni tildes, en los dos sentidos ("cafe" encuentra "café" y "río" encuentra "rio"). En un paquete se buscan su nombre y su descripción, no los de sus actividades. Sin los espacios de los extremos, el texto tiene entre 2 y 100 caracteres; si no, 400 `DATO_INVALIDO`. El orden sigue siendo por precio y luego por id.
- **Dónde vive:** `TextoBusqueda`, en el dominio, normaliza el texto y arma una expresión regular por palabra: cada vocal, la `n` y la `c` aceptan sus variantes con tilde, en minúscula y mayúscula, y los caracteres especiales se escapan. El adaptador en memoria evalúa esas expresiones con `java.util.regex` y el de MongoDB las envía como `$regex` en la primera etapa de la agregación de DC-31, así que los dos dan los mismos resultados; el contrato común de los repositorios lo verifica.
- **Por qué no `$text`:** el índice de texto de MongoDB solo encuentra palabras completas (no sirve mientras el turista escribe), ordena por relevancia en lugar de por precio y MongoDB admite uno solo por colección. La expresión regular no exige campos nuevos ni recrear la base del piloto.
- **Costo:** la expresión regular no usa un índice; se evalúa sobre las ofertas que ya dejaron pasar el índice parcial de DC-17 y los filtros de tipo, zona y precio. Con el catálogo del piloto no pesa; la prueba de carga de QAS-07 lo confirma, y si no alcanza se cambia a un índice de texto solo en el adaptador de MongoDB, sin tocar el dominio.

## Moderación

**DC-38. El catálogo muestra lo que espera moderación; el núcleo decide.** Como en el diagrama del módulo de administración del núcleo, el administrador lee lo pendiente en el catálogo, decide en el núcleo (`ModerarOfertaUseCase`, RF-79) y la decisión vuelve al catálogo como aviso de moderación por la bandeja de salida (DC-04). El catálogo no tiene una ruta para aprobar o rechazar.
- **`GET /api/catalogo/admin/moderacion/ofertas`** (`ModeracionController`, `ListarPendientesModeracionUseCase`), con el rol ADMINISTRADOR (DC-13). Lista las ofertas no retiradas cuya versión pendiente está en moderación (`Oferta.estaEnModeracion`), nuevas o ediciones, de la enviada hace más tiempo a la más reciente y luego por id, paginadas. Las rechazadas no aparecen: esperan a que el prestador edite (DC-06).
- Cada una trae el contenido de la versión en moderación tal como lo envió el prestador, la versión publicada si es una edición, para comparar (RF-66), el número de versión que nombra la decisión (DC-05) y la copia del RNT del prestador, porque el núcleo no aprueba si no está activo.
- **`GET /api/catalogo/internal/moderacion/ofertas/{id}`** es para el núcleo: antes de decidir necesita el prestador (para sus reglas de aprobación y para notificar el rechazo), el nombre de la versión en moderación y su número. La consulta interna de DC-35 no sirve, porque una oferta nueva en moderación no es visible. Responde 404 si la oferta no existe, está retirada o no tiene una versión en moderación.
- En MongoDB, el índice parcial `en_moderacion` (`{pendiente.enviadaEn, _id}`, solo con las ofertas en moderación) resuelve la lista sin recorrer la colección.
- Las opiniones pendientes (RF-80) las lista el módulo de opiniones, con la misma forma: `GET /api/catalogo/admin/moderacion/opiniones`.

## Perfil público y fotos

**DC-39. El prestador gestiona su perfil y sus fotos viven en MinIO.** `GestionarPerfilPublicoUseCase` (RF-64, RF-72) y `FileStoragePort` con `MinioStorageAdapter`, como en el diagrama del módulo.
- **Rutas del prestador** (rol PRESTADOR, DC-13): `GET` y `PUT /api/catalogo/prestador/perfil`, `POST /api/catalogo/prestador/perfil/fotos` (multipart, parte `foto`) y `DELETE /api/catalogo/prestador/perfil/fotos/{fotoId}`. Siempre es el perfil del prestador de la identidad: la ruta no lleva su id, así que nadie toca el perfil de otro. Hay que crear el perfil (con su descripción) antes de subir fotos; si no existe, 404 `PERFIL_NO_ENCONTRADO`.
- **Sin moderación:** los cambios del perfil se publican de inmediato. El SRS solo exige moderar actividades (RF-66) y opiniones (RF-80). La descripción no tiene límite de palabras.
- **Fotos:** JPG, PNG o WebP de hasta 5 MB, como máximo 6 por perfil (el mismo límite tendrán las ofertas). El formato se reconoce por el contenido del archivo, no por su extensión. Nunca se guarda el archivo subido: `ProcesadorImagenes` lo decodifica ya reducido (para que la memoria no dependa de los megapíxeles de la cámara), lo endereza según su orientación EXIF y guarda dos JPG nuevos, sin metadatos (sin la ubicación GPS del celular): `galeria`, de hasta 1280 px por lado, y `miniatura`, de hasta 400 px, para la red móvil (QAS-07, RT-03). Se rechaza una imagen que dice tener más de 100 megapíxeles, sin decodificarla.
- **Identificadores:** cada foto es un UUID aleatorio (`Foto`), nunca el nombre del archivo subido; el catálogo guarda solo ese id en el perfil, y la API arma las rutas. Así no se pueden adivinar ni salirse de la carpeta.
- **Entrega:** `GET /api/catalogo/fotos/{fotoId}/{galeria|miniatura}`, pública. MinIO no se expone hacia afuera: el catálogo es la única puerta. Una foto nunca cambia (cambiarla es subir otra), así que se entrega con `Cache-Control: public, max-age=31536000, immutable` y `X-Content-Type-Options: nosniff`.
- **Consistencia:** el perfil tiene revisión y bloqueo optimista con reintento, como las ofertas (ADR-05): dos fotos subidas a la vez no se pisan. La foto se guarda en el almacenamiento antes que en el perfil; si el perfil no se puede guardar, se borra para no dejarla huérfana. Quitar una foto la saca primero del perfil y después del almacenamiento; quitarla dos veces no es un error. Desde DC-47 cada foto del perfil también queda en el registro de fotos.
- **Adaptadores:** `MinioStorageAdapter` con el perfil `minio` (Docker Compose), que crea el bucket al arrancar; sin él, `ArchivoLocalStorageAdapter` guarda en una carpeta local (`catalogo.fotos.carpeta`), útil para correr sin Docker con una sola réplica. Los dos cumplen el mismo contrato de pruebas.
- **Imagen de MinIO:** MinIO dejó de publicar imágenes en Docker Hub y en Quay. Se usa la que compila Chainguard desde el código de MinIO (`cgr.dev/chainguard/minio`), que corre sin root; su plan gratuito solo ofrece la etiqueta `latest`.
- **Límites de subida:** Spring acepta archivos de hasta 5 MB, Tomcat lee hasta 10 MB de una subida rechazada para poder responder el 400, y Nginx acepta cuerpos de hasta 6 MB (por defecto acepta 1 MB). Si el cuerpo pasa de 6 MB, responde Nginx con el mismo 400 `DATO_INVALIDO` del catálogo, en lugar de su página HTML 413.
- **Datos de demostración:** los perfiles arrancan sin fotos, y las URLs de ejemplo que tenían se quitaron. Una base del piloto creada antes de este cambio no tiene la revisión en `perfiles`: hay que recrearla con `docker compose down -v`.

## Opiniones

**DC-40. El módulo `opiniones` consulta las ofertas solo por `catalogo.api`.** Las reglas 4 y 5 de `ArquitecturaTest` permiten que un módulo use de otro solo su paquete `api`, sin ciclos.
- **La API:** `catalogo.api.ConsultaOfertaApi` responde si una oferta existe, si es visible y su nombre. La implementa `ConsultaOfertaApiAdapter`, un adaptador de entrada del catálogo (`infraestructura/entrada/modulos`) que llama al caso de uso `ConsultarOfertaParaModulosUseCase`; la visibilidad es la misma de la ficha y de la consulta del núcleo (DC-35).
- **Del lado de opiniones:** la capa de aplicación solo puede depender del dominio (regla 2), así que el módulo usa su puerto de salida `OfertaCatalogoPort` y su adaptador `OfertaCatalogoAdapter`, el único que importa `catalogo.api`. Si el catálogo se separa en otro servicio, solo cambia ese adaptador.
- **El catálogo no depende de opiniones:** la ficha (`GET /ofertas/{id}`) no trae las opiniones; el frontend las pide a `GET /ofertas/{id}/opiniones`.
- **Lo que el módulo tiene propio** por la misma regla: `Pagina`, `Paginacion`, las excepciones, `ResultadoAviso`, el registro de avisos procesados, los índices (`IndicesOpinionesMongo`), los datos de demostración (`DatosOpiniones`) y su carga (`CargaDemoOpinionesMongo`). Tienen la misma forma que los del catálogo, pero `IndicesMongo`, `DatosSantaMarta` y `CargaDemoMongo` no pueden incluir opiniones sin que el catálogo dependa de ellas. Los errores los traduce `ManejadorErroresOpiniones` con los códigos de DC-15; el JSON ilegible lo sigue respondiendo el manejador del catálogo, que vale para todo el servicio.

**DC-41. La elegibilidad para opinar llega del núcleo por aviso** ([SUPUESTO], RF-60). "Tener una solicitud pagada de la oferta" es un dato del núcleo. Cuando una solicitud queda Pagada, el núcleo envía por la bandeja de salida de DC-04 el aviso `HABILITACION_OPINION {idAviso, turistaId, ofertaId}` a `POST /api/catalogo/internal/opiniones/avisos`, y el módulo guarda la habilitación en `habilitaciones_opinion`, una por turista y oferta.
- Sin habilitación, crear la opinión responde 403 `OPINION_NO_HABILITADA`. No hay llamadas síncronas al núcleo: el catálogo sigue respondiendo aunque el núcleo no esté (ADR-04).
- Una habilitación repetida, o de una oferta que el catálogo no tiene, se ignora (`IGNORADO`).
- Una oferta que se retira después del pago se puede seguir calificando, porque existe; sus opiniones no se leen en público mientras no sea visible (DC-43).

**DC-42. Validaciones y estados de la opinión** ([SUPUESTO] hasta cerrar el SRS).
- La calificación es un entero de 1 a 5 y es obligatoria. El comentario es obligatorio y tiene de 10 a 1000 caracteres sin los espacios de los extremos. Si no, 400 `DATO_INVALIDO` con el campo.
- La opinión nace `EN_MODERACION`. Se aprueba o se rechaza solo desde ese estado; en otro, la decisión se ignora, como DC-08. Rechazar exige motivo.
- Una opinión rechazada **no se edita** en esta versión: el turista la ve con su motivo en "Mis opiniones" y, como solo hay una por turista y oferta (RF-61), no puede enviar otra. `TODO` para el SRS: ¿el turista puede corregir una opinión rechazada?

**DC-43. Lectura pública de las opiniones** (RF-59). `GET /api/catalogo/ofertas/{id}/opiniones`:
- Solo las aprobadas, de la más reciente a la más antigua y luego por id, con la paginación de DC-14.
- Trae un resumen de todas las aprobadas: el total y el promedio de la calificación con un decimal, redondeado hacia arriba desde la mitad. El promedio lo calcula `ResumenCalificaciones` en el dominio; la base solo cuenta y suma, así que los dos adaptadores redondean igual. Sin opiniones aprobadas no se envía el promedio.
- No devuelve el turista ([SUPUESTO]): el catálogo no tiene su nombre y su id no le sirve a otro turista. El frontend muestra "Turista".
- Si la oferta no es visible, responde 404 `OFERTA_NO_ENCONTRADA`, igual que la ficha.
- El turista ve las suyas en `GET /api/catalogo/turista/opiniones`, en cualquier estado, con el nombre de la oferta y el motivo si fue rechazada.

**DC-44. Moderación de opiniones: el catálogo lista, el núcleo decide** (RF-80), como las ofertas en DC-38.
- **`GET /api/catalogo/admin/moderacion/opiniones`** (`ModeracionOpinionesController`), con el rol ADMINISTRADOR: las opiniones en moderación, de la enviada hace más tiempo a la más reciente y luego por id, con el nombre de la oferta (una consulta a `catalogo.api` por página). No trae el turista: el administrador no lo necesita para decidir.
- **`GET /api/catalogo/internal/moderacion/opiniones/{id}`** es para el núcleo: antes de decidir necesita el turista para notificarle. Responde 404 `OPINION_NO_ENCONTRADA` si la opinión no existe o ya no está en moderación.
- La decisión llega como aviso `MODERACION_OPINION {idAviso, opinionId, decision, motivo}`. No lleva número de versión porque una opinión no se edita (DC-42). Es idempotente y usa el bloqueo optimista del ADR-05: si otra escritura se cruza, el aviso completo se reintenta.

**DC-45. Supresión de las opiniones de un turista** (derechos del titular, Ley 1581 de 2012). Cuando el núcleo ejecuta la supresión de un turista, envía `SUPRESION_TURISTA {idAviso, turistaId}` por la bandeja de salida.
- El módulo **borra** sus opiniones y sus habilitaciones; no las anonimiza ([SUPUESTO]: el derecho de supresión pide eliminar el dato). El resumen de cada oferta se ajusta solo, porque se calcula al leer.
- Responde `APLICADO` si borró algo e `IGNORADO` si el turista no tenía nada; una decisión de moderación que llegue después encuentra la opinión borrada y se ignora.
- Límite: un aviso de habilitación del mismo turista que llegue después de la supresión volvería a crear la habilitación. El núcleo no debe emitirlo, porque el turista suprimido ya no tiene solicitudes.

**DC-46. Persistencia de opiniones.**
- **Colecciones:** `opiniones {_id, ofertaId, turistaId, calificacion, comentario, fecha, estado, motivoRechazo?, revisadaEn?, revision}`, con `ofertaId` como `ObjectId` y `turistaId` como UUID (DC-30); `habilitaciones_opinion {_id, turistaId, ofertaId}`; y `avisos_opiniones_procesados {_id = idAviso}`, separada de la de ofertas para que cada módulo lleve su propio registro.
- **Índices** (`IndicesOpinionesMongo`): `opinion_unica`, único `{ofertaId, turistaId}` (RF-61), que rechaza la segunda opinión aunque llegue a otra réplica; `opiniones_aprobadas`, parcial con `estado: APROBADA`, `{ofertaId, fecha desc, _id}`, para la lectura pública y el resumen; `opiniones_en_moderacion`, parcial con `estado: EN_MODERACION`, `{fecha, _id}`, para la cola del administrador; `opiniones_del_turista`, `{turistaId, fecha desc, _id}`, para "Mis opiniones" y la supresión; y `habilitacion_unica`, único `{turistaId, ofertaId}`. `OpinionMongoAdapterIT` verifica con `explain()` que cada lectura usa su índice.
- **Concurrencia:** revisión explícita como en DC-33: una opinión nueva se inserta con la revisión 1 y una moderada se reemplaza con el filtro `{_id, revision}`.
- **Pruebas:** `ContratoOpinionRepository` son las mismas pruebas para los adaptadores en memoria y de MongoDB, con los datos de demostración.
- **Datos de demostración** (`DatosOpiniones`): 9 opiniones aprobadas en 4 ofertas visibles, 2 en moderación y 1 rechazada, cada una con su habilitación. Los turistas demo Laura (`70000000-…-000000000007`) y Andrés (`80000000-…-000000000008`) se eligen en el frontend; Laura además pagó dos ofertas que no ha calificado. Se cargan en MongoDB con las mismas reglas de DC-27.

## Fotos de las ofertas

**DC-47. Las ofertas llevan de 1 a 6 fotos, que pasan por moderación; las que nadie usa se borran.** El SRS solo pide fotos en el perfil (RF-64, RF-72); el equipo decidió que las ofertas también las tengan, con los mismos límites del perfil (DC-39).
- **Parte de la versión:** las fotos están en `ContenidoComun`, en orden; la primera es la portada. Cambiarlas es editar la oferta: pasa por moderación y, mientras tanto, el turista sigue viendo las aprobadas (RF-66). El administrador las ve en la lista de moderación, las nuevas junto a las publicadas (DC-38).
- **Cantidad:** de 1 a 6 por versión, sin repetir. La foto obligatoria se exige al publicar y al editar, no al leer: una versión guardada antes de esta decisión, sin fotos, se sigue leyendo y mostrando.
- **Subida:** `POST /api/catalogo/prestador/fotos` (multipart, parte `foto`, rol PRESTADOR) con el mismo procesamiento de DC-39; responde 201 con el id y las direcciones. Al publicar o editar, `OfertaRequest.fotos` lleva los ids. Subir no exige el RNT activo, porque no publica nada; publicar y editar sí (DC-11).
- **Varias ofertas:** una foto puede ir en varias ofertas del mismo prestador; por ejemplo, la de una actividad en el paquete que la incluye.
- **Registro de fotos** (`FotoSubidaRepositoryPort`, colección `fotos`): de cada foto guarda el prestador, el destino (`PERFIL` u `OFERTAS`) y su último uso. Una oferta solo acepta fotos que su prestador subió para sus ofertas; una de otro prestador, una del perfil o una que ya se borró responde 400 `DATO_INVALIDO` en `fotos`. Las fotos del perfil no sirven en las ofertas: así, quitar una del perfil la borra en seguida sin el riesgo de que una oferta la esté usando.
- **Limpieza** (`LimpiarFotosSinUsoUseCase`): una tarea programada (`LimpiezaFotosProgramada`) corre cada hora, en el minuto 17 (`catalogo.fotos.limpieza.cron`). Borra las fotos cuyo último uso pasó hace más de 24 horas y que no tiene ninguna oferta no retirada, ni en la versión publicada ni en la pendiente, ni ningún perfil:
  - las que se subieron y nunca se pusieron en una oferta;
  - las de versiones reemplazadas por una edición o por una aprobación;
  - las de ofertas retiradas: el retiro es definitivo (RF-65) y nadie las vuelve a ver (decisión del equipo).

  Una foto que sigue en uso se marca como vista en uso, para no revisarla otra vez hasta que pasen otras 24 horas. Recorre el registro en lotes de 100, como máximo 50 por ejecución.
- **Plazo para usar una foto:** quien sube una foto tiene 24 horas para ponerla en una oferta; después puede ya no existir y hay que subirla de nuevo.
- **Sin carreras entre asignar y borrar:** asignar marca el uso antes de comprobar que la foto sigue registrada, y borrar solo borra el registro si el uso no cambió desde que lo leyó (un borrado condicionado). Si la limpieza la borra primero, la oferta no la encuentra y responde 400; si la oferta la marca primero, la limpieza no la borra.
- **Una sola réplica limpia:** con el perfil `mongo` las réplicas comparten los datos, así que la tarea toma un candado de ShedLock en MongoDB (colección `shedLock`). Lo mantiene al menos 5 minutos, para que la otra réplica no la repita si su reloj va unos segundos atrasado, y como mucho 30, por si la réplica se cae. Con el perfil `memoria` cada réplica tiene sus datos y no hay candado. Se usa ShedLock 6.10, la última versión para Spring Boot 3.5; la 7 exige Spring Boot 4.
- **Orden de escritura:** el registro se crea antes que el archivo y se borra antes que él. Si algo falla en medio, queda un registro sin uso, que la limpieza borra, o un archivo sin registro, que no ve nadie y queda anotado en el log.
- **Entrega:** la ficha trae la galería (`fotos`), y la búsqueda y la lista del prestador traen la portada (`portada`), con la miniatura para las tarjetas (QAS-07). Las fotos de una versión en moderación se entregan igual que las demás, por su id, que no se puede adivinar. Restringirlas no protegería nada, porque el perfil, que no tiene moderación, ya publica fotos sin revisión.
- **Datos de demostración:** cada oferta demo tiene una foto, con un id que sale de su nombre, registrada a nombre de su prestador; así un prestador puede editar su oferta demo y conservar la foto. Con el perfil `demo`, `CargaFotosDemo` guarda en el almacenamiento las que falten, desde `src/main/resources/demo/fotos`; si falta una imagen, el servicio arranca igual y esa foto responde 404.
- **Base del piloto creada antes de DC-47:** sus ofertas, sin fotos, siguen funcionando, pero no tienen las fotos de demostración. Para tenerlas hay que recrear la base con `docker compose down -v`.

## Rendimiento

**DC-48. Evidencia de QAS-07: métricas en el servidor y prueba de carga de la búsqueda.** QAS-07 pide p95 ≤ 2 s medido en el servidor, con 500 usuarios concurrentes que buscan por tipo, zona y precio. QAS-02 pide, con la misma carga, menos de 1 % de errores y al menos 50 peticiones por segundo durante 10 minutos. La prueba cubre la parte de los dos que le toca al catálogo: la búsqueda.
- **Métricas en el servidor** (Micrometer con `micrometer-registry-prometheus`): `/actuator/prometheus` publica `http_server_requests_seconds` por ruta, con umbrales de 50 ms a 3 s. Cada umbral cuenta, desde el arranque, cuántas peticiones respondieron en ese tiempo; los contadores se suman entre réplicas, y "p95 ≤ 2 s" se cumple si al menos el 95 % de las búsquedas cae en el umbral de 2 s. No se publica el percentil que calcula cada réplica: el formato de Prometheus no lo admite junto a los umbrales, y no se puede sumar entre réplicas.
- **Solo dentro de Docker:** Nginx ya bloquea `/actuator/**`, así que las métricas no se exponen al público. No se agregaron Prometheus ni Grafana (decisión del equipo): RNF-13 pide además alertas y retención de 30 días, que quedan pendientes.
- **Datos** (`DatosDeCarga`, perfil `carga`, `CargaVolumenMongo`): 5.000 ofertas sintéticas de 200 prestadores inventados, 25 cada uno, en las 7 zonas, construidas con las operaciones del dominio como las de la demostración. La mezcla imita un catálogo en uso para que los índices parciales de DC-17 trabajen: cerca del 7 % sin aprobar, el 5 % retiradas, el 8 % de las actividades con una edición en moderación y 10 prestadores sin RNT. Quedan visibles 4.044 de las 5.000, más las 21 de la demostración. Los identificadores empiezan por `d`, salen de una semilla fija y la carga es idempotente entre réplicas, como DC-27; corre después de la carga de demostración.
- **Carga** (`infra/carga/busqueda-catalogo.js`, k6 1.8.1): 500 usuarios virtuales, un minuto de subida y 10 minutos sostenidos. Cada uno busca con tipo, zona y rango de precio al azar, a veces con texto (20 %) o en la segunda página (15 %), y espera de 1 a 3 s antes de la siguiente búsqueda. Pasa por Nginx a las dos réplicas, de 512 MB cada una. Al empezar y al terminar, k6 lee las métricas de cada réplica y calcula el resultado del servidor solo con las búsquedas de la prueba.
- **Entorno:** `infra/docker-compose.carga.yml`, un proyecto aparte (`samarian-carga`) con sus propios volúmenes, para que las ofertas sintéticas no lleguen a la base de la demostración. Todo corre en un portátil: AMD Ryzen 3 7320U (4 núcleos, 8 hilos), 13,7 GB de RAM, Docker Desktop con 8 CPU y 6,6 GB. k6 corre en la misma máquina y le quita CPU al catálogo, así que el resultado es conservador.
- **Resultado** (2026-10-23):

  | Medida | Resultado | Meta |
  |---|---|---|
  | Búsquedas | 158.845, a 248,6 por segundo | ≥ 50 por segundo (QAS-02) |
  | Errores | 0 (0,00 %) | < 1 % (QAS-02) |
  | Servidor, dos réplicas | 158.840 de 158.847 en 2 s o menos (99,996 %); el 98,9 % en 50 ms o menos, así que el p95 está por debajo de 50 ms | p95 ≤ 2 s (QAS-07) |
  | Cliente, pasando por Nginx | p95 29 ms, p99 95 ms, máximo 2,8 s | — |

- **Sin ajustes:** con los índices parciales (DC-17), el tope de la paginación (DC-14) y la copia del RNT en cada oferta (DC-03) no hizo falta cambiar el código ni la configuración. Nginx, con sus 512 conexiones por defecto, no dio errores.
- **Límites:** no es la nube del piloto, sino un portátil. Solo mide la búsqueda: ni la ficha, ni las miniaturas de las tarjetas, ni el envío de solicitudes, que es del núcleo y QAS-02 también incluye. El p95 de 3 s de extremo a extremo en red móvil se mide cuando exista el frontend. k6 registró una duración negativa (−1,2 s): un salto del reloj de la máquina virtual de Docker Desktop, que quizá explica también el máximo; no cambia los percentiles.

## Gestión de zonas

**DC-49. El administrador registra, edita, marca e inactiva las zonas; nunca se eliminan** (HU-ADM-06, RF-84). La zona es el lugar o atractivo donde se ubica la oferta; su ficha muestra las buenas prácticas (RF-25, HU-TUR-16).
- **Dónde:** `/admin/zonas` en el catálogo, con el rol ADMINISTRADOR (DC-13). El administrador escribe directo en el catálogo, que es el dueño de las zonas; a diferencia de la moderación (DC-38), no pasa por el núcleo: las interacciones entre servicios del documento de diseño no incluyen las zonas.
- **Operaciones** (`GestionarZonasUseCase`): `GET /admin/zonas` lista todas, activas e inactivas; `POST` registra una zona, que nace activa, y responde 201 con su ficha en `Location`; `PUT /admin/zonas/{zonaId}` reemplaza todos sus datos y las marcas. En el `PUT`, `activa` es obligatoria para que inactivar o reactivar sea explícito.
- **Datos** (`DatosZona`, decisión del equipo): nombre, descripción y ubicación son obligatorios, los tres que nombra la historia; las buenas prácticas (una por elemento, sin elementos en blanco) y las condiciones de visita son opcionales, y las marcas que no se envían quedan en false. `Zona` no exige estas reglas al leer, porque una zona guardada antes puede no cumplirlas.
- **Nombre único** (decisión del equipo): sin distinguir mayúsculas, tildes ni espacios repetidos (`Zona.claveNombre`, con la misma normalización de la búsqueda por texto, DC-37); otro igual responde 409 `ZONA_DUPLICADA`. En MongoDB lo garantiza el índice único `zona_por_nombre` sobre `nombreClave`, aunque dos réplicas guarden a la vez. Las zonas guardadas antes de DC-49 reciben `nombreClave` al crear los índices.
- **Inactivar** (decisión del equipo): la zona sale de `GET /zonas` y no admite ofertas nuevas ni ediciones (DC-12, `ZONA_NO_DISPONIBLE`), pero las ofertas que ya tiene **siguen visibles**, en la búsqueda y en su ficha. Por eso la visibilidad (`ReglaVisibilidad`) no cambia y la búsqueda sigue usando solo el índice de ofertas (QAS-07). Una zona inactiva se puede **reactivar**. Nunca se elimina: tampoco una que nadie usa, para no dejar referencias rotas en las solicitudes y los indicadores del núcleo, que guardan la zona.
- **Ficha de la zona:** `GET /zonas/{zonaId}` responde también por una inactiva, con `activa` en false, porque sus ofertas siguen visibles; `ZONA_NO_ENCONTRADA` si no existe.
- **Efecto inmediato:** las marcas y las buenas prácticas se ven en cuanto se guardan, porque la ficha de la oferta y la consulta de la recomendación (DC-36) leen la zona al responder; no hay copias en las ofertas que actualizar.
- **Concurrencia:** cada guardado reemplaza el documento completo de una vez. Si dos administradores editan a la vez, queda la última edición completa; no hay estado intermedio que se pueda corromper.
- **Alternativas por alta afluencia (RF-29, HU-TUR-17):** las calcula la recomendación, como dice el diseño (decisión del equipo); el catálogo le entrega la marca en la consulta interna (DC-36).

## Oferta propia completa

**DC-50. El prestador consulta su oferta completa, también la que nunca se publicó** (RF-65, RF-66, RF-79). La lista de ofertas propias solo trae el resumen de cada versión (nombre, precio, portada y estado), y la ficha pública solo existe para una oferta visible. Por eso el frontend llenaba el formulario de edición con la ficha pública, y una oferta rechazada que nunca se aprobó no se podía corregir desde la interfaz.
- **`GET /prestador/ofertas/{id}`** (`GestionarOfertaUseCase.consultarMia`): la oferta en cualquier estado, incluida la retirada, con la versión publicada y la pendiente completas. Cada versión trae su número, estado, motivo del rechazo y fechas, y el contenido con los mismos campos que ve el administrador al moderarla (`ContenidoVersion`, DC-38): los de su tipo, las fotos y la unidad del precio.
- **Solo su dueño:** la de otro prestador responde 403 `PRESTADOR_NO_AUTORIZADO` y una que no existe, 404 `OFERTA_NO_ENCONTRADA`, como al editarla o retirarla (DC-15).
- **Unidad del precio en la lista:** cada versión de `GET /prestador/ofertas` trae `unidadPrecio`, como la búsqueda (DC-22); el frontend ya no tiene que deducirla del tipo.

## Cambios pendientes en los documentos de diseño

1. **Vista general de paquetes, interacción 2:** quitar "tipo de prestador" de los avisos del núcleo al catálogo (DC-01).
2. **Diagrama de paquetes, estructura del repositorio:** cambiar "Java 25" por "Java 21" (DC-20).
3. **Diagrama de clases y diccionario:**
   - agregar `PrestadorCatalogo` como clase de réplica del catálogo (DC-02);
   - agregar `rntPrestadorActivo` en `Oferta` (DC-03) y `numero` en `VersionOferta` (DC-05);
   - cambiar `duracionMinutos : int` por `Integer` en `VersionActividad`, y dejar `modalidad` y `puntoEncuentro` como opcionales: solo son obligatorios para el guía (DC-10);
   - quitar `esAccesibleSillaRuedas()` de los métodos abstractos de `VersionOferta` y `nivelEsfuerzo()` de `VersionPaquete`: actividad, alojamiento y restaurante guardan su accesibilidad, y los valores del paquete se calculan con sus actividades en `ReglasPaquete` (DC-24);
   - agregar `actividadIds` en `VersionPaquete`: el paquete referencia sus actividades por el identificador de su oferta;
   - agregar `prestadorId` y `nombreComercial : String` (opcional) en `PerfilPublico` (DC-26).
4. **Modelo documental (MongoDB)** (sección 2 del documento de diseño):
   - quitar la base `recomendacion` (DC-32);
   - marcar `publicada` como `| null`: una oferta nueva solo tiene versión pendiente (DC-06);
   - marcar `ubicacion` y `tipoUnidad` como opcionales mientras el SRS no diga lo contrario (DC-19);
   - agregar `nombreComercial` (texto, opcional) en la colección `perfiles` (DC-26);
   - agregar los índices: los dos parciales de búsqueda y `{prestadorId, _id}` en `ofertas` (DC-17), único en `perfiles.prestadorId` y único en `{ofertaId, turistaId}` de `opiniones` (RF-61);
   - agregar en `opiniones` los campos `estado`, `motivoRechazo`, `revisadaEn` y `revision`, sus índices parciales y el de `turistaId`, y las colecciones `habilitaciones_opinion` y `avisos_opiniones_procesados` (DC-46);
   - usar el mismo nombre de base que el diagrama de despliegue y Docker Compose: `catalogo_db`.
5. **Diagrama del módulo catálogo:** agregar el puerto de salida `PrestadorCatalogoRepositoryPort` (réplica de DC-02) con su adaptador `PrestadorMongoAdapter`.
6. **Agrupaciones técnicas del código** que no cambian el modelo, por si el docente compara: `ContenidoComun` agrupa los atributos comunes de `VersionOferta`, `RegistroModeracion` agrupa número, estado, motivo y fechas de moderación de cada versión, y `OfertaVisible` reúne una oferta con lo que la recomendación necesita de ella (DC-36).
7. **Diagrama de clases, módulo de opiniones:** si la clase `Opinion` no los tiene, agregar `estado : EstadoOpinion` (EN_MODERACION, APROBADA, RECHAZADA), `motivoRechazo` y `revisadaEn`, ambos opcionales, y aclarar que `fecha` es la de envío. Agregar `HabilitacionOpinion {turistaId, ofertaId}`, la réplica de las solicitudes pagadas (DC-41). `revision` es técnica (ADR-05) y `ResumenCalificaciones` agrupa el total y la suma de las calificaciones (DC-43); no cambian el modelo.
8. **Diagrama de paquetes:** agregar `catalogo.api.ConsultaOfertaApi` y la dependencia `opiniones → catalogo.api` (DC-40).
9. **Vista general, bandeja de salida del núcleo:** agregar los avisos `HABILITACION_OPINION` (solicitud pagada), `MODERACION_OPINION` y `SUPRESION_TURISTA` hacia el catálogo (DC-41, DC-44, DC-45).
10. **SRS, RF-85 y RF-88:** cambiar "tipo de actividad" por "tipo de oferta", como ya lo hizo el CU-09 del Entregable 2; la solicitud no guarda el tipo de actividad (DC-35).
11. **SRS, RF-65 y RF-20:** agregar las fotos de la oferta: de 1 a 6, moderadas con la versión, y la galería en la ficha (DC-47).
12. **Diagrama de clases y diccionario:** agregar `fotos : List<Foto>` en `VersionOferta` (en el código, en `ContenidoComun`) y las clases `Foto`, `FotoSubida` y `DestinoFoto` (DC-39, DC-47).
13. **Diagrama del módulo catálogo:** agregar el puerto de salida `FotoSubidaRepositoryPort` con `FotoSubidaMongoAdapter`, los casos de uso `SubirFotoUseCase` y `LimpiarFotosSinUsoUseCase`, y el adaptador de entrada programado `LimpiezaFotosProgramada` (DC-47).
14. **Modelo documental (MongoDB):** agregar la colección `fotos` (`_id` texto, `prestadorId`, `destino`, `ultimoUso`), el campo `fotos` (ids en texto) en las versiones de `ofertas`, la colección `shedLock` y los índices `fotos_publicadas`, `fotos_pendientes`, `fotos_del_perfil` y `fotos_por_ultimo_uso` (DC-47).

15. **SRS, RF-60 y RF-61 (desviación conocida):** RF-60 permite opinar "sin exigir una solicitud previa ni confirmación de asistencia", como decidió el equipo en la HU-ADM-03 del Entregable 1, y RF-61 permite editar y reenviar una opinión rechazada. El código exige una solicitud pagada (DC-41) y no permite editar la opinión rechazada (DC-42). El equipo decidió dejar el código como está: hay que cambiar los RF o el módulo de opiniones.
16. **Documento de diseño, verificación de QAS-07 y QAS-02:** citar la prueba de carga de la búsqueda como evidencia de la parte del catálogo, con su entorno y sus límites (DC-48).
17. **Diagrama del módulo catálogo:** agregar el caso de uso `GestionarZonasUseCase` con `GestionarZonasService`, `ConsultarZonasUseCase.consultar`, las operaciones de escritura de `ZonaRepositoryPort` (`guardar`, `buscarTodas`, `nuevoId`) y el adaptador de entrada `ZonaAdminController` (DC-49). `DatosZona` es una agrupación técnica como `ContenidoComun`: no cambia el modelo.
18. **Modelo documental (MongoDB):** agregar en `zonas` el campo `nombreClave` y su índice único `zona_por_nombre` (DC-49).
19. **Diagrama del módulo catálogo:** agregar la operación `consultarMia` a `GestionarOfertaUseCase` (DC-50).

## Fuera del alcance del prototipo

La pantalla de moderación del administrador, que vive en el núcleo; las alternativas por alta afluencia, que son de la recomendación (RF-29); la regla de modalidades del guía (RF-69); Prometheus, Grafana y las alertas de RNF-13; la medición de extremo a extremo en red móvil de QAS-07.
