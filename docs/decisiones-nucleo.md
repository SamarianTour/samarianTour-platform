# Decisiones del prototipo: núcleo transaccional

Registro de las decisiones tomadas para el servicio `nucleo` (módulos 1 Cuentas, 4 Solicitudes, 5 Pagos, 7 Prestadores, 8 Administración y 9 Indicadores, sobre PostgreSQL). Complementa el documento de diseño, el diagrama de paquetes y el diccionario de clases; donde los contradice, se indica en la sección final. Sigue el formato de `decisiones-catalogo-ofertas.md`.

- **Estado:** vigente, octubre de 2026. La base del servicio, la moderación (DN-08), las solicitudes de disponibilidad (DN-09 a DN-17) y las cuentas (DN-28 a DN-32); cada módulo agrega sus decisiones con el número siguiente.
- **Convención:** ✅ decisión que sale de un requisito, del diseño o de una decisión del catálogo (se cita la fuente). 🆕 decisión nueva del equipo.

## Resumen

| # | Decisión | Origen |
|---|---|---|
| DN-01 | El esquema de PostgreSQL lo crea Flyway | 🆕 equipo |
| DN-02 | Persistencia con Spring Data JPA, con el dominio separado de las entidades | 🆕 equipo, ADR-02 |
| DN-03 | Siempre PostgreSQL: no hay perfil con los datos en memoria | 🆕 equipo |
| DN-04 | Los cinco roles del SRS | ✅ SRS, roles de usuario |
| DN-05 | Identidad provisional por headers y autorización por prefijo de ruta, como el catálogo | ✅ DC-01, DC-13, DC-15 |
| DN-06 | Bandeja de salida hacia el catálogo | ✅ documento de diseño, DC-04 |
| DN-07 | Estructura del servicio y despliegue en el piloto | ✅ diagrama de paquetes, diagrama de despliegue |
| DN-08 | Moderación de ofertas y opiniones: el núcleo decide y avisa al catálogo | ✅ RF-79, RF-80, DC-38, DC-44 |
| DN-09 | Ciclo de vida de la solicitud: nueve estados y sus transiciones en el dominio | ✅ SRS 3.1.3, RF-30 a RF-45, RF-58 |
| DN-10 | Ventanas y anticipación en el dominio, no en la configuración | ✅ QAS-05, RF-30, RF-36, RF-39, RF-41, RF-55 |
| DN-11 | Persistencia de las solicitudes, no duplicado y bloqueo optimista | ✅ RF-31, DN-02, ADR-05 |
| DN-12 | Claves de idempotencia en enviar y responder | ✅ ADR-04 · 🆕 [SUPUESTO] 422 y 24 h |
| DN-13 | Notificaciones guardadas en la transacción del cambio | ✅ RF-34, RF-41, RF-44, RF-45, RF-56, RF-57, DN-06 |
| DN-14 | Vencimientos programados con SKIP LOCKED | ✅ RF-41, RF-55, RF-56 · 🆕 [SUPUESTO] cada minuto |
| DN-15 | Prestadores y contactos provisionales de la demo | 🆕 [SUPUESTO] |
| DN-16 | Cola del operador | ✅ RF-32, RF-34 a RF-36 |
| DN-17 | API de las solicitudes para pagos | ✅ documento de diseño, RF-50, RF-55, RF-58 |
| DN-28 | Modelo de las cuentas, nombre y WhatsApp cifrados, administrador inicial y cuentas de la demo | ✅ RF-01 a RF-03, RF-09 a RF-11, RNF-08, diccionario · 🆕 [SUPUESTO] validaciones y 3 intentos |
| DN-29 | Sesión: JWT RS256 de 15 min y token de renovación rotativo de 7 días; bloqueo con la fila bloqueada | ✅ RF-04 a RF-06, diagrama de cuentas · 🆕 vigencias |
| DN-30 | Restablecer la contraseña por correo, con Mailpit en el piloto | ✅ RF-07, RF-08 · 🆕 equipo |
| DN-31 | Datos personales y supresión inmediata del turista, con anonimización | ✅ RF-12, RF-13, Ley 1581 · 🆕 equipo |
| DN-32 | API de las cuentas para los demás módulos: el contacto del turista | ✅ diagrama de paquetes, RF-44, RF-57 |

## Persistencia

**DN-01. El esquema lo crea Flyway.** Las migraciones están en `servicios/nucleo/src/main/resources/db/migration` y se aplican al arrancar. Hibernate solo valida que las entidades coincidan con el esquema (`ddl-auto: validate`): nunca crea ni cambia tablas.
- **Nombres:** `V<n>__<descripcion_en_minusculas>.sql`, con el número siguiente al último en `develop`. Una migración en Java, como V9, es una clase con el mismo nombre registrada como bean de Spring. Una migración ya fusionada no se edita: un cambio va en una migración nueva.
- **Varias réplicas:** las dos réplicas pueden arrancar a la vez. Flyway serializa las migraciones con un candado de PostgreSQL, así que cada una se aplica una sola vez.
- **V1** crea la bandeja de salida (DN-06); **V2**, el historial de moderación (DN-08), **V3**, las solicitudes con su historial, las claves de idempotencia y las notificaciones (DN-11 a DN-13); **V4**, los campos de cada tipo de oferta y el índice de no duplicado con la hora de la reserva (DN-09, DN-11); **V5**, el nombre `SOLICITUD_PAGADA` de las notificaciones ya guardadas (DN-13), **V6**, el aparte del cupo (DN-09); **V8**, las cuentas, y **V9**, el administrador inicial (DN-28). V7 la reserva otra rama.

**DN-02. Spring Data JPA, con el dominio separado.** Las entidades JPA y los repositorios viven en `infraestructura/salida/persistencia` de cada módulo (regla 8 de `ArquitecturaTest`), y el adaptador convierte entre la entidad y el modelo del dominio, que no conoce JPA (regla 1). Es el mismo esquema que el catálogo con sus documentos de MongoDB. `open-in-view` está apagado: cada caso de uso carga lo que necesita.

**DN-03. Siempre PostgreSQL.** A diferencia del catálogo, el núcleo no tiene un perfil `memoria`: arrancarlo exige PostgreSQL, y en Docker Compose ya lo tiene. Así cada puerto de salida tiene un solo adaptador. Las pruebas unitarias de la aplicación usan dobles en memoria escritos en la prueba, y las de integración, un PostgreSQL de Testcontainers con la misma versión del piloto (`IntegracionPostgresBase`).

## Seguridad

**DN-04. Los cinco roles del SRS** (`compartido/seguridad/Rol`): `TURISTA`, `PRESTADOR`, `ADMINISTRADOR`, `OPERADOR_DISPONIBILIDAD` y `GESTOR_DESTINO`. El catálogo solo distingue los tres primeros. Los nombres son los que debe llevar el token.

**DN-05. Identidad provisional y autorización por prefijo, como el catálogo** (DC-01, DC-13, DC-15).
- Mientras no exista `libs/seguridad-jwt`, la identidad llega en los headers `X-Usuario-Id`, `X-Rol` y, para el prestador, `X-Tipo-Prestador`, solo con el perfil `demo` (`IdentidadPorHeaders`). El JWT la reemplaza con otra implementación de `ResolutorIdentidad`; lo demás no cambia.
- Cada prefijo exige un rol (`SeguridadConfig`): `/turista/**`, `/prestador/**`, `/admin/**`, `/operador/**` y `/gestor/**`; `/cuenta/**` admite los cinco (DN-31). Sin identidad, 401 `NO_AUTENTICADO`; con otro rol, 403 `ACCESO_DENEGADO`.
- El núcleo ya emite el token (DN-29). En el PR de `seguridad-jwt`, la validación del token en los tres servicios pasa a Spring Security (servidor de recursos con la clave pública), como dice el diagrama de paquetes (`compartido/seguridad: Spring Security + libs/seguridad-jwt`), y reemplaza a `ExigirRol` con los mismos 401 y 403.
- `compartido` es una copia del del catálogo (identidad, reloj, `X-Instancia`, reintento ante conflictos del ADR-05). Cada servicio se despliega por separado, así que no comparten código; la única biblioteca común será `libs/seguridad-jwt`.

## Bandeja de salida

**DN-06. Bandeja de salida hacia el catálogo** (documento de diseño: "BandejaSalidaAdapter guarda los avisos para el catálogo en la misma base y en la misma transacción que el cambio que los origina"; DC-04 del catálogo). Es el módulo `bandeja`.
- **Encolar:** un módulo que origina un aviso lo encola desde su adaptador de salida con `bandeja.api.BandejaSalidaApi`, dentro de su transacción. La regla 2 no deja a `aplicacion` usar otro módulo, así que el módulo declara su propio puerto de salida y su adaptador llama a la API, como hace opiniones con `catalogo.api` (DC-40). `encolar` exige una transacción abierta (`MANDATORY`): si el cambio se deshace, el aviso tampoco queda. Un `idAviso` repetido no se duplica (`ON CONFLICT DO NOTHING`), y el catálogo descarta los repetidos por el mismo identificador.
- **Destinos:** las dos entradas del catálogo, `OFERTAS` (`POST /internal/avisos`) y `OPINIONES` (`POST /internal/opiniones/avisos`). El cuerpo es el aviso como lo describe el contrato del catálogo, guardado en JSONB.
- **Publicar:** una tarea programada corre cada 15 s (decisión del equipo, `nucleo.bandeja.publicacion.cron`). Toma lotes de 10 avisos pendientes cuyo intento ya toca y los envía con una espera de 3 s (RNF-05); sigue lote tras lote mientras haya, hasta 50 lotes por ejecución.
- **Dos réplicas** (decisión del equipo): cada lote se toma con `FOR UPDATE SKIP LOCKED`, así que las dos publican en paralelo y nunca envían el mismo aviso a la vez. No hace falta ShedLock.
- **Respuestas del catálogo:**
  - 2xx: enviado. El catálogo responde 200 tanto si lo aplicó como si ya lo tenía o no le correspondía.
  - Otro 4xx: rechazado, sin reintentos, con el error guardado y anotado en el log (decisión del equipo): reintentar no arregla un aviso mal formado.
  - 409: fallido, se reintenta. Es un conflicto de concurrencia que el catálogo no resolvió con sus reintentos, y el mismo catálogo pide reenviarlo (ADR-05).
  - 5xx, tiempo agotado o sin conexión: fallido, se reintenta.
- **Reintentos** (decisión del equipo): sin límite, con espera creciente (`EsperaCreciente`): 5 s tras el primer fallo y el doble tras cada uno, hasta 5 minutos. Si el catálogo pasa un rato caído, ningún aviso se pierde.
- **Pendiente:** las filas enviadas y rechazadas se conservan como registro; no hay todavía una limpieza ni una forma de reenviar un rechazado, más allá de cambiar su estado a mano.

## Administración

**DN-08. Moderación de ofertas y opiniones** (RF-79, RF-80; DC-38 y DC-44 del catálogo). El administrador lee lo pendiente en el catálogo (`GET /api/catalogo/admin/moderacion/ofertas` y `/opiniones`), decide en el núcleo y la decisión vuelve al catálogo como aviso por la bandeja (DN-06). Es el módulo `administracion`, con un servicio por caso de uso: `ModerarOfertaService` y `ModerarOpinionService`.
- **Rutas**, con el rol ADMINISTRADOR; responden 200 con la decisión registrada:
  - `POST /api/nucleo/admin/moderacion/ofertas/{ofertaId}/decision` con `{numeroVersion, decision, motivo}`. La versión es la que trae la lista del catálogo (DC-05): así una decisión nunca cae sobre una edición que el administrador no vio.
  - `POST /api/nucleo/admin/moderacion/opiniones/{opinionId}/decision` con `{decision, motivo}`, sin versión, porque una opinión no se edita (DC-42).
  - `decision` es `APROBAR` o `RECHAZAR`. Rechazar exige `motivo`, como en el catálogo (DC-08). El motivo tiene como máximo 1000 caracteres, el mismo tope del comentario de una opinión (decisión del equipo, provisional como DC-19). Un dato inválido responde 400 `DATO_INVALIDO`.
  - El administrador que decide es el de la identidad (DN-05), nunca un campo del cuerpo.
- **Antes de decidir** el núcleo consulta al catálogo (`/internal/moderacion/ofertas/{id}` y `/internal/moderacion/opiniones/{id}`), con el mismo cliente y la misma espera de 3 s que la bandeja (`compartido/config/ClienteCatalogoConfig`, RNF-05). De ahí saca a quién notificar y comprueba que la decisión todavía aplica:
  - no está en moderación (no existe, está retirada o ya se decidió en el catálogo): 404 `NO_ESTA_EN_MODERACION`;
  - la versión pedida no es la que está en moderación, porque el prestador editó mientras tanto: 409 `VERSION_NO_VIGENTE`, con las dos versiones en el mensaje;
  - el catálogo no responde, se agota la espera o responde un error que no es 404: 503 `CATALOGO_NO_DISPONIBLE`, sin registrar ni encolar nada (decisión del equipo). El administrador lo intenta de nuevo.
- **Historial** (decisión del equipo): cada decisión queda en la tabla `decisiones_moderacion` (V2), con el administrador, la fecha, el motivo y el `idAviso`. Hay una sola por oferta y versión, o por opinión: `UNIQUE NULLS NOT DISTINCT (tipo, moderado_id, numero_version)`, que trata la versión vacía de las opiniones como un valor.
- **Decisiones repetidas** (decisión del equipo):
  - la misma decisión responde 200 con la registrada, con su fecha y su motivo, sin consultar al catálogo ni encolar otro aviso. Un administrador que reintenta tras un error de red no ve un error;
  - la contraria responde 409 `MODERACION_YA_DECIDIDA`: una decisión no se cambia;
  - si dos réplicas reciben la misma oferta a la vez, el `INSERT ... ON CONFLICT DO NOTHING` deja pasar a una sola, y la otra responde como si la suya fuera repetida.
- **Aviso:** en la misma transacción que el registro se encola el aviso del contrato del catálogo:
  - `MODERACION {idAviso, ofertaId, numeroVersion, decision, motivo}` hacia `OFERTAS`, con `idAviso` `moderacion-oferta-<id>-v<n>`;
  - `MODERACION_OPINION {idAviso, opinionId, decision, motivo}` hacia `OPINIONES`, con `idAviso` `moderacion-opinion-<id>`.

  El catálogo lo aplica en los siguientes 15 s. Los identificadores son estables, y el catálogo descarta un aviso repetido (DC-08).
- **Notificación** (decisión del equipo, provisional): al rechazar, el núcleo notifica el motivo al prestador (RF-79) o al turista (RF-80) por `NotificacionPort`. Mientras no exista un canal de notificaciones, el adaptador solo lo escribe en el log (`[Notificación simulada] ...`). La aprobación no se notifica: el prestador la ve en sus ofertas y el turista, en sus opiniones.
- **Pendiente:** DC-38 dice que el núcleo no aprueba una oferta si el RNT del prestador no está activo. La consulta interna del catálogo no trae el RNT, y la habilitación es del módulo de prestadores (RF-62, RF-63), que todavía no existe: la regla entra con ese módulo.

## Solicitudes de disponibilidad

**DN-09. Ciclo de vida de la solicitud** (SRS 3.1.3, RF-30 a RF-45, RF-58). Es el módulo `solicitudes` (módulo 4 del SRS, módulo 02 del diagrama de paquetes). `SolicitudDisponibilidad` es el agregado, en Java puro.
- **Nueve estados** (`EstadoSolicitud`): PENDIENTE, ATENDIDA, NO_DISPONIBLE, SIN_RESPUESTA, CONFIRMADA, DECLINADA, PAGO_FALLIDO, PAGADA y VENCIDA. Son terminales NO_DISPONIBLE, SIN_RESPUESTA, DECLINADA, VENCIDA y PAGADA.
- **Transiciones válidas**, en el agregado; cualquier otra responde 409 `TRANSICION_INVALIDA`:

  | Desde | Hacia | Quién |
  |---|---|---|
  | PENDIENTE | ATENDIDA, NO_DISPONIBLE | el operador |
  | PENDIENTE | SIN_RESPUESTA | el sistema, a las 24 h |
  | ATENDIDA | CONFIRMADA, DECLINADA | el turista |
  | ATENDIDA | VENCIDA | el sistema, al cerrar la ventana |
  | CONFIRMADA | PAGADA, PAGO_FALLIDO | pagos |
  | CONFIRMADA | VENCIDA | el sistema |
  | PAGO_FALLIDO | PAGADA, PAGO_FALLIDO (otro rechazo) | pagos |
  | PAGO_FALLIDO | VENCIDA | el sistema, o el tercer rechazo |
- **Historial:** cada cambio deja una `TransicionSolicitud` (de, a, en, actor, motivo), con la creación como primera (sin estado de origen). El turista lo ve sin el identificador del actor. Nunca guarda el detalle de la negociación con el prestador (RF-40).
- **Al crearse** (RF-30): la oferta se consulta al catálogo (`GET /internal/ofertas/{id}`) y se copian su tipo, nombre, prestador y zona (DC-35). Es la única llamada de red del flujo: usa el cliente compartido (espera de 3 s, RNF-05) y el circuito `catalogo` de Resilience4j (5 fallas en 10 consultas lo abren por 30 s [SUPUESTO]). Un 404 es `OFERTA_NO_ENCONTRADA` y no cuenta como falla; un error, la espera agotada o el circuito abierto son 503 `CATALOGO_NO_DISPONIBLE`, sin crear nada (ADR-04).
- **Datos del turista:**
  - fecha de inicio (DN-10);
  - los campos propios del tipo de oferta (RF-30, `CamposDelTipo`):
    - actividad y paquete: `horaSalida`, una de sus `horariosSalida`;
    - alojamiento: `fechaSalida`, posterior a la fecha de inicio, y `numeroUnidades`, entero positivo;
    - restaurante: `horaReserva`. El catálogo no publica el horario del restaurante en un formato que se pueda validar (es texto libre), así que se acepta cualquier hora [SUPUESTO];
    - un campo que falta, o uno que no corresponde al tipo, es 400 `DATO_INVALIDO`;
  - personas, de 1 a la capacidad máxima de la oferta, en todos los tipos;
  - nota opcional de hasta 500 caracteres [SUPUESTO].
- **Lo que registra el operador** (RF-37 a RF-39): para marcarla Atendida hacen falta el precio base (entero positivo en pesos, sin cargos adicionales), la política de cancelación del prestador y `aparteAcordado: true`. El SRS exige las tres (RF-39: "solo cuando haya registrado el precio base, el aparte y las condiciones, incluida la política de cancelación") y el diccionario de clases tiene `politicaCancelacion` y `aparteAcordado`. El aparte queda registrado en `aparteHasta` (V6), con la vigencia de la ventana (RF-38): atendida a las 15:00, hasta las 18:00; a las 20:30, hasta las 08:00 del día siguiente. A diferencia de `venceEn`, no se vacía al cerrar la solicitud, y el turista lo ve con las condiciones. Una nota opcional lleva otras condiciones. Después de Atendida los datos no cambian.
- **Sin tiempo para confirmar** (RF-39): si la ventana, con su gracia de 30 min, terminaría después del inicio del servicio, la solicitud no puede marcarse Atendida (409 `SIN_TIEMPO_PARA_CONFIRMAR`), solo No disponible. Con 2 días de anticipación y 24 h para el operador, en la práctica pasa con la ventana nocturna: atendida a las 21:00 para una salida a las 07:00 del día siguiente.
- **Plazos vencidos:** pasado el plazo, nadie la cambia aunque el vencimiento programado todavía no haya corrido (409 `PLAZO_VENCIDO`): ni el operador después de las 24 h ni el turista después de la ventana.
- **Pagos** (RF-50, RF-58): hasta tres intentos. Un rechazo deja Pago fallido mientras queden intentos y la ventana siga abierta; el tercero, o uno que llega durante la gracia, la vence. Un pago aprobado sobre una solicitud ya vencida no la revive (DN-17).

**DN-10. Ventanas y anticipación en el dominio** (QAS-05: cambiar las 24 h o las 3 h solo toca una clase). Por eso los valores están en `PoliticaVentanas` y no en la configuración. Todos corren desde el cambio de estado, no desde que la notificación llega al turista (ADR-04, decisión ratificada).
- **24 h para el operador**, desde la creación (RF-41).
- **Ventana de confirmación y pago** (RF-55), desde la transición a Atendida, en la hora de Colombia:
  - de 05:00 a 18:59, 3 h después;
  - de 19:00 a 23:59, a las 08:00 del día siguiente;
  - de 00:00 a 04:59, a las 08:00 del mismo día.
- **Gracia de 30 min** con un intento de pago en curso (RF-55): la solicitud no vence mientras el intento siga en curso, hasta 30 min después del fin de la ventana.
- **Próxima a vencer** (RF-36): una pendiente con 6 h o menos para atenderla (valor ratificado).
- **Anticipación** (`PoliticaAnticipacion`, RF-30): la fecha de inicio debe ser al menos 2 días calendario después del envío, en la hora de Colombia. Son días de calendario, no 48 h.
- **Inicio del servicio:** la fecha de inicio a la hora que aplica a su tipo: la de salida (actividad y paquete) o la de la reserva (restaurante). El alojamiento no tiene hora: las 00:00 de ese día [SUPUESTO], el caso más estricto para RF-39.
- **El reloj** es el `Clock` inyectado de `compartido`, en la zona de Colombia: las pruebas fijan la hora (bordes 18:59, 19:00, 23:59, 00:00, 04:59 y 05:00 en `PoliticaVentanasTest`).

**DN-11. Persistencia de las solicitudes, no duplicado y bloqueo optimista** (RF-31, DN-02, ADR-05).
- **Tablas (V3):** `solicitudes` y `transiciones_solicitud`, que solo agrega filas. V4 agrega a `solicitudes` `hora_reserva`, `fecha_salida` y `numero_unidades`, y V6, `aparte_hasta`. Índices:
  - la cola: `(operador_id, estado, creada_en)`;
  - los vencimientos: `limite_vencimiento` de las abiertas, parcial;
  - las del turista: `(turista_id, creada_en DESC)`.
- **No duplicado** (RF-31): `ReglaNoDuplicado` revisa las abiertas del turista para la misma oferta, fecha de inicio y hora, y responde 409 `SOLICITUD_DUPLICADA` con la que ya existe. La hora es la de su tipo: la de salida o la de la reserva; el alojamiento se compara solo por la fecha de inicio, aunque cambie la fecha de salida. Cuando dos envíos llegan a la vez, quizá a réplicas distintas, la regla no alcanza a ver al otro: lo detiene el índice único parcial `solicitudes_no_duplicada` sobre `(turista_id, oferta_id, fecha_inicio, hora_salida, hora_reserva)` de las no terminales (V4, que reemplaza el de V3), con `NULLS NOT DISTINCT` para que el alojamiento, sin hora, también cuente.
- **Bloqueo optimista** (ADR-05): la revisión es la `@Version` de JPA. El adaptador compara la revisión leída y, si otra transacción la cambió, lanza `ConflictoConcurrenciaException`.
- **Reintento:** los servicios reintentan el caso de uso completo, cada intento en su propia transacción. Para eso la aplicación usa `TransaccionPort` (una `TransactionTemplate` en la infraestructura) en lugar de `@Transactional`: una transacción que falló por el bloqueo optimista ya no se puede confirmar, y con la anotación el reintento quedaría dentro de ella.
- **Cruce entre el operador y el vencimiento:** si el operador atiende justo cuando el vencimiento corre, gana uno. El otro, al reintentar, recarga la solicitud y ve su estado real (409 `TRANSICION_INVALIDA`), o el vencimiento ya no la encuentra vencida. Lo prueba `VencimientosIT`.

**DN-12. Claves de idempotencia** (ADR-04): enviar la solicitud y responder las condiciones exigen el header `Idempotency-Key` (un UUID que la aplicación genera antes del primer intento y repite en cada reintento). Sin él, o si no es un UUID, 400 `DATO_INVALIDO`.
- **Tabla `claves_idempotencia`:** una por usuario y clave, con la operación, la huella (SHA-256 de la operación, el recurso y el cuerpo convertido de nuevo a JSON, así no importan los espacios) y la respuesta original, el código y el cuerpo tal cual.
- **En una sola transacción** (`EjecucionIdempotente`): se reserva la clave con `INSERT ... ON CONFLICT DO NOTHING`, como DN-08, se ejecuta la operación y se guarda su respuesta. Si dos réplicas reciben la misma clave a la vez, PostgreSQL hace esperar a la segunda en el índice hasta que la primera termine, y entonces la segunda lee la respuesta guardada.
- **Misma clave y mismo cuerpo:** la respuesta original, con `Idempotency-Replayed: true`, sin crear ni cambiar nada.
- **Misma clave con otro cuerpo** [SUPUESTO]: 422 `CLAVE_IDEMPOTENCIA_REUTILIZADA`.
- **Solo se guardan las respuestas exitosas** (decisión del equipo). Si la operación falla, por ejemplo porque el catálogo no respondió, la transacción se deshace con la reserva y el reintento con la misma clave vuelve a intentarlo, como pide el ADR-04. Un error 4xx se vuelve a evaluar igual.
- **Vigencia de 24 h** [SUPUESTO]: después, la misma clave cuenta como nueva. Una tarea horaria borra las vencidas (`nucleo.solicitudes.idempotencia.cron`).

**DN-13. Notificaciones guardadas en la transacción del cambio** (RF-34, RF-41, RF-44, RF-45, RF-56, RF-57, como la bandeja de DN-06).
- **Quién recibe qué:** el agregado produce las notificaciones de cada transición (`ReglaNotificaciones`):

  | Cambio | Turista (WhatsApp) | Prestador (WhatsApp) | Operador (plataforma) |
  |---|---|---|---|
  | Creada (Pendiente) | SOLICITUD_RECIBIDA (RF-44) [SUPUESTO] | | NUEVA_SOLICITUD (RF-34) |
  | Atendida | SOLICITUD_ATENDIDA: oferta, precio base, política, plazo y enlace a la confirmación (RF-45) | | |
  | No disponible, Confirmada, Pago fallido | uno por cambio (RF-44) | | |
  | Sin respuesta | SOLICITUD_SIN_RESPUESTA (RF-41) | | |
  | Declinada, Vencida | SOLICITUD_DECLINADA / SOLICITUD_VENCIDA | CUPO_LIBERADO (RF-56) | |
  | Pagada | SOLICITUD_PAGADA | SOLICITUD_PAGADA, con el nombre, la fecha, la hora, las personas y el WhatsApp del turista (RF-57) | |
- **Una solicitud pagada:** el turista y el prestador reciben la misma `SOLICITUD_PAGADA`; el destinatario decide los datos. Se llamaba `RESERVA_PAGADA` para el prestador, pero en este proyecto el turista no reserva: envía una solicitud de disponibilidad (glosario del SRS). V5 renombra las que ya estaban guardadas.
- **Los datos de la plantilla** llevan los campos del tipo de oferta: `horaSalida` u `horaReserva`, o `fechaSalida` y `numeroUnidades`.
- **Guardadas con el cambio:** `notificaciones_solicitud` se escribe en la misma transacción que el cambio de estado, así nunca se notifica algo que no quedó guardado. Una tarea programada envía las de WhatsApp cada 10 s [SUPUESTO] con `FOR UPDATE SKIP LOCKED`: las dos réplicas envían en paralelo sin repetir. Si un envío falla, se reintenta tras 1, 2, 4 y 8 min, y al quinto intento queda Fallida.
- **Sin datos de contacto guardados:** la notificación guarda el destinatario (tipo e identificador) y los parámetros de la plantilla, nunca un número. El WhatsApp se busca al enviar (DN-15), y al prestador de una solicitud pagada se le agregan entonces el nombre y el WhatsApp del turista.
- **Avisos del operador** [SUPUESTO]: son el mismo registro con el canal PLATAFORMA (`AvisoPlataformaPort`), entregados al confirmar la transacción y consultables en `GET /operador/avisos`. El operador es personal interno sin WhatsApp en la demo, así que solo recibe avisos en la plataforma.
- **WhatsApp simulado:** mientras no exista la integración con WhatsApp Business, `WhatsappSimuladoAdapter` escribe en el log el destinatario enmascarado (solo sus últimos 4 dígitos) y el tipo de mensaje. La de Atendida muestra el enlace a la confirmación (`nucleo.solicitudes.enlace-confirmacion` [SUPUESTO]), que solo lleva el identificador de la solicitud. Nunca escribe el número completo ni los parámetros.

**DN-14. Vencimientos programados** (RF-41, RF-55, RF-56).
- **Cada minuto** [SUPUESTO] (`nucleo.solicitudes.vencimiento.cron`), `VencimientoProgramado` toma lotes de 50 solicitudes abiertas cuyo vencimiento ya toca, con `FOR UPDATE SKIP LOCKED`: las dos réplicas vencen en paralelo sin tomar la misma y sin ShedLock. Aplica Sin respuesta o Vencida según el estado, con su transición y sus notificaciones en la misma transacción.
- **`limite_vencimiento`:** lo calcula el dominio en cada cambio: el plazo vigente, o 30 min después si hay un intento de pago en curso. Así la consulta solo compara fechas y la gracia no se repite en SQL (QAS-05).
- **Con la fila bloqueada,** el operador o el turista que la cambien a la vez esperan, chocan con la revisión y ven el estado real (DN-11).

**DN-15. Prestadores y contactos provisionales** [SUPUESTO]. El operador asignado y el WhatsApp de cada prestador son del módulo de prestadores, que todavía está vacío. El contacto del turista ya sale de su cuenta (DN-32); la tabla de turistas que estaba aquí se quitó.
- **Tabla de la demo:** `PrestadoresPort` y `ContactoPort` tienen adaptadores provisionales que leen `nucleo.demo` en `application-demo.yml`, con los identificadores de la demo del catálogo y de sus opiniones:
  - la agencia (`10000000-...-001`) y la guía (`20000000-...-002`) las atiende el operador `60000000-...-001`;
  - el hotel (`30000000-...-003`) y el restaurante (`40000000-...-004`), el `60000000-...-002`;
  - los turistas `90000000-...-001` a `...003` ya no están en la tabla: son cuentas de la demo (DN-28).
- **Sin el perfil demo** la tabla queda vacía y ninguna solicitud se puede enviar.
- **Errores:** un prestador sin operador responde 409 `PRESTADOR_SIN_OPERADOR` y deja la incidencia en el log, con el prefijo `[INCIDENCIA RF-32]`, el prestador y la oferta, para que el administrador le asigne uno (RF-32). Un turista sin WhatsApp verificado responde 409 `WHATSAPP_NO_VERIFICADO` (RF-30).
- **Cuando existan los módulos,** se cambian por adaptadores que llamen a su `api`, sin tocar el dominio.

**DN-16. Cola del operador** (RF-32, RF-34 a RF-36).
- **Asignación estática** (RF-32): cada solicitud queda con el operador vinculado al prestador al crearse. El operador solo ve y atiende las suyas; la de otro operador responde 404, igual que la de otro turista, para no revelar que existe.
- **La cola:** `GET /operador/solicitudes` da sus pendientes de la más antigua a la más reciente (RF-35), con `venceEn` y `proximaAVencer` (RF-36). Con `?estado=`, su historial de trámites, de la más reciente a la más antigua.
- **El detalle** trae el WhatsApp del prestador [SUPUESTO], porque el operador lo contacta fuera de la plataforma (RF-71). El turista no lo ve.
- **Los avisos** de la plataforma (RF-34) están en `GET /operador/avisos` (DN-13).

**DN-17. API de las solicitudes para pagos** (documento de diseño: "PagoService invoca a SolicitudService en la misma transacción"). `solicitudes.api.SolicitudesPagosApi`, como `bandeja.api`, implementada aquí y probada (`SolicitudesPagosApiIT`); la usará la rama de pagos.
- **`consultarParaCobro(solicitudId, turistaId)`:** estado, precio base, fin de la ventana, prestador y oferta, solo si el turista es el dueño y está Confirmada o con el pago fallido; si no, vacío.
- **Eventos:** `marcarIntentoEnCurso`, `registrarPagoAprobado` y `registrarPagoRechazado` devuelven `APLICADO`, `SIN_CAMBIOS` (el evento ya se había aplicado) o `PAGO_TARDIO` (un aprobado sobre una solicitud ya vencida, que no se revive: pagos lo registra y avisa al administrador, RF-50).
- **Sin transacción propia:** los eventos exigen la de pagos (`MANDATORY`), para que el pago y la solicitud se confirmen juntos, y no reintentan: ante `ConflictoConcurrenciaException` reintenta pagos con su transacción completa.
- **Errores:** si la solicitud no existe o su estado no admite el evento, lanzan `EventoPagoNoAplicableException` con el código (`SOLICITUD_NO_ENCONTRADA`, `TRANSICION_INVALIDA`, `PLAZO_VENCIDO`).
- **El api es Java puro:** tiene sus propios tipos (`SolicitudParaCobro`, `ResultadoPago`), y el adaptador de `infraestructura/entrada/modulos` traduce los del dominio, así pagos no depende del dominio de solicitudes. Lo comprueban dos reglas nuevas de `ArquitecturaTest`: la 10 (el `api` no depende de dominio, aplicación, infraestructura ni frameworks) y la 11 (solo `infraestructura/entrada/modulos` lo implementa).
- **Pendiente para la rama de pagos:** el aviso `HABILITACION_OPINION` al catálogo cuando una solicitud queda Pagada (DC-41) y la notificación al administrador del pago tardío.

## Cuentas

**DN-28. Modelo de las cuentas, datos cifrados y cuentas iniciales** (RF-01 a RF-03, RF-09 a RF-11, RNF-06, RNF-08, diccionario de clases). Es el módulo `cuentas`, con los casos de uso del diagrama de paquetes.
- **Modelo:**
  - `Usuario` es abstracta, con una subclase por rol (`Turista`, `Prestador`, `OperadorDisponibilidad`, `Administrador`, `GestorDestino`), como en el diccionario de clases.
  - En la base de datos es una sola tabla, `usuarios`, con la columna `rol`.
  - Solo el turista tiene datos propios en este módulo: su WhatsApp y si lo verificó. El RNT, el tipo y el WhatsApp del prestador son del módulo de prestadores.
- **Migraciones:**
  - **V8** crea `usuarios`, `politicas_datos` con la versión `v1`, `aceptaciones_politica`, `solicitudes_supresion`, `codigos_un_uso` y `sesiones`.
  - V7 queda libre para la migración de otro compañero; Flyway admite el hueco.
- **Cifrado en reposo** (RNF-08, decisión del equipo):
  - el nombre y el WhatsApp van cifrados con AES-256-GCM (`compartido/cifrado/CifradorDatos`), que el módulo de prestadores reutilizará para las cuentas de cobro;
  - el correo queda en claro y en minúsculas, porque es con lo que se inicia sesión;
  - el adaptador JPA cifra al guardar y descifra al leer; el dominio nunca ve un valor cifrado.
- **Claves:**
  - `NUCLEO_CIFRADO_CLAVE` no tiene valor por defecto: sin ella el núcleo no arranca, en lugar de guardar datos sin cifrar.
  - `bash infra/generar-claves.sh` la agrega a `infra/.env`, junto con la del token (DN-29). Solo agrega las que faltan, porque **cambiar la clave de cifrado deja ilegibles los datos ya guardados**.
- **Contraseñas:** se guardan con BCrypt, de costo 10 (`spring-security-crypto`). Los secretos de un solo uso (códigos, enlaces y tokens de renovación) usan el mismo hash.
- **Administrador inicial** (RF-01, decisión del equipo):
  - lo crea **V9**, una migración de Flyway en Java (`V9__crear_administrador_inicial`), porque su nombre se guarda cifrado con la clave del entorno;
  - es `admin@samariantour.co`, con la contraseña inicial `SamarianAdmin2026` y el identificador `60000000-...-006` del administrador de la demo del catálogo;
  - Flyway la aplica una sola vez por base. La contraseña está en el repositorio, así que se cambia con el restablecimiento por correo (DN-30) al instalar.
- **Cuentas de la demo** (`PrecargaCuentasDemo`, solo con el perfil `demo`):
  - 13 cuentas con los identificadores del catálogo, del frontend y de la demo de solicitudes: 5 prestadores, 2 operadores, un gestor (`60000000-...-003`, nuevo), Laura (`7000...`), Andrés (`8000...`) y los turistas `9000...001` a `003`;
  - todas con la contraseña `Demo1234`, correos `@demo.samariantour.co` y la política aceptada; los turistas, con su WhatsApp ficticio verificado;
  - se crean al arrancar si no existen.
- **Registro** (`POST /cuentas/registro`, público):
  - turista o prestador (RF-01); exige `aceptaPolitica` y la versión vigente (RF-09, RF-10), y queda la constancia;
  - el correo es único sin importar las mayúsculas (409 `CORREO_YA_REGISTRADO`);
  - el turista registra su WhatsApp y el prestador no;
  - la política se lee en `GET /politica-datos/vigente` y su texto en `GET /politica-datos/v1`. Ese texto es de demostración [SUPUESTO] y declara la transferencia del nombre y el WhatsApp del turista al prestador (RF-11).
- **Verificación del WhatsApp** (RF-03):
  - el registro envía un código de 6 dígitos, que vale 30 minutos (RF-08) y admite 3 intentos (decisión del equipo);
  - `POST /turista/whatsapp/codigo` envía otro, y solo sirve el último;
  - sin verificar, el turista no envía solicitudes (RF-30);
  - el WhatsApp es simulado, como en DN-13: el log muestra el número enmascarado y el código, para poder verificar en la demo.
- **Datos de la cuenta** [SUPUESTO], como DC-19:
  - nombre de hasta 100 caracteres;
  - correo de hasta 254;
  - WhatsApp en formato E.164 (`+57...`);
  - contraseña de 8 a 72 caracteres con una letra y un número (BCrypt ignora lo que pasa de 72 bytes).

**DN-29. Sesión: token de acceso JWT RS256 y token de renovación rotativo** (RF-04 a RF-06, decisión del equipo).
- **Rutas:** `POST /cuentas/sesion`, `/cuentas/sesion/renovacion` y `/cuentas/sesion/cierre`, públicas.
- **Token de acceso** (`JwtRs256Adapter`):
  - es un JWT RS256 de **15 minutos**, firmado con `NimbusJwtEncoder` de Spring Security;
  - lleva `iss` (`samarian-tour-nucleo`), `sub` (el usuario), `rol`, `sid` (la sesión), `iat`, `exp` y, en la cabecera, `kid` (la huella de la clave);
  - los demás servicios lo validarán con la clave pública, sin consultar al núcleo (PR de `seguridad-jwt`);
  - la clave privada llega en `NUCLEO_JWT_CLAVE_PRIVADA`, en PKCS#8 y Base64, generada por `infra/generar-claves.sh`. Las dos réplicas usan la misma.
- **Token de renovación:**
  - es opaco, `<sesión>.<secreto>`, y de la sesión solo se guarda el hash del secreto;
  - dura **7 días desde su último uso** y rota en cada renovación;
  - si llega uno que ya rotó, se revoca la sesión completa: o se filtró, o lo usó otro cliente;
  - va en el cuerpo de la respuesta, no en una cookie.
- **Bloqueo** (RF-05): cinco fallos consecutivos bloquean 15 minutos (`PoliticaBloqueo`).
  - El inicio de sesión lee la cuenta con `SELECT ... FOR UPDATE`, así que los intentos de una misma cuenta se atienden uno tras otro, aunque lleguen a réplicas distintas, y ninguno se escapa del conteo.
  - Mientras dura el bloqueo, ni la contraseña correcta entra: 423 `CUENTA_BLOQUEADA`, con `bloqueadaHasta`.
- **Sin revelar cuentas:**
  - un correo inexistente y una contraseña equivocada responden lo mismo (401 `CREDENCIALES_INVALIDAS`);
  - con un correo inexistente igual se compara contra un hash ficticio, para que el tiempo de respuesta no lo delate;
  - una cuenta desactivada solo se informa después de verificar la contraseña (403 `CUENTA_DESACTIVADA`).
- **Cierre** (RF-06): revoca la sesión; el token de acceso ya emitido vence solo, en 15 minutos como máximo. Con un token que no sirve también responde 204.
- **Pendiente para el PR de `seguridad-jwt`:**
  - el token todavía no lleva el tipo de prestador, que el catálogo necesita y que es del módulo de prestadores;
  - mientras tanto, las rutas protegidas siguen leyendo los headers de la demo (DN-05).

**DN-30. Restablecer la contraseña por correo, con Mailpit en el piloto** (RF-07, RF-08, decisión del equipo).
- **`POST /cuentas/contrasena/restablecimiento` `{correo}`:**
  - responde 202 exista o no la cuenta, para no revelar qué correos están registrados;
  - a una cuenta activa le envía un enlace al frontend, `/restablecer-contrasena?token=<id>.<secreto>` (`nucleo.cuentas.enlace-restablecimiento`);
  - el enlace es un `CodigoUnUso` de 30 minutos y un solo uso; pedir otro invalida los anteriores.
- **`POST /cuentas/contrasena` `{token, contrasena}`:**
  - cambia la contraseña, quita el bloqueo por intentos (quien abre el enlace demostró que el correo es suyo) y cierra todas las sesiones;
  - un enlace que no sirve responde 409 `CODIGO_NO_VIGENTE`, y una contraseña débil, 400, sin gastar el enlace.
- **Correo por SMTP** (`SmtpEmailAdapter`), con 3 s de espera (RNF-05):
  - en el piloto el servidor es **Mailpit** (`axllent/mailpit:v1.21`), que muestra los correos en `http://localhost:8025` y no entrega nada a direcciones reales;
  - el correo sale dentro de la transacción: si el SMTP falla, responde 503 `CORREO_NO_DISPONIBLE` y no queda guardado un enlace que nunca llegó;
  - el chequeo de salud del correo está apagado: sin correo el núcleo sigue sirviendo y Docker no lo marca como enfermo.

**DN-31. Datos personales y supresión inmediata del turista** (RF-12, RF-13, Ley 1581, decisiones del equipo).
- **`GET /cuenta` y `PUT /cuenta`**, para cualquier rol (por eso `/cuenta/**` admite los cinco, DN-05):
  - se cambian el nombre y, el turista, el WhatsApp; uno distinto queda sin verificar y recibe un código nuevo;
  - el correo no se cambia por ahora, porque habría que verificar el nuevo.
- **`POST /turista/supresion` `{contrasena}`:** solo el turista en este PR; el prestador queda para el módulo de prestadores, porque sus ofertas viven en el catálogo.
  - Se confirma con la contraseña; el fallo cuenta para el bloqueo de RF-05, así que esta ruta no sirve para adivinarla.
  - Es **inmediata**, en una sola transacción:
    - el nombre pasa a "Titular suprimido";
    - el correo pasa a `suprimido-<id>@cuentas.invalid`, para que el titular pueda volver a registrarse con el suyo;
    - se borran la contraseña y el WhatsApp, y la cuenta queda `SUPRIMIDA`;
    - se cierran sus sesiones y se encola `SUPRESION_TURISTA` para que el catálogo borre sus opiniones (DC-45).
- **Lo que se conserva:** la constancia (`solicitudes_supresion`, `EJECUTADA`, con sus fechas) y las aceptaciones de la política, que no tienen datos personales y son la evidencia de RNF-09. Las solicitudes del turista conservan solo su UUID: se suprime aunque tenga solicitudes abiertas, porque las cuentas no dependen de otro módulo.

**DN-32. API de las cuentas para los demás módulos** (diagrama de paquetes: `CuentasApiInterna`, RF-44, RF-57).
- **La API:** `cuentas.api.CuentasApiInterna.contactoTurista(id)` da el nombre y el WhatsApp solo si la cuenta está activa y el WhatsApp verificado.
  - La implementa `infraestructura/entrada/modulos/CuentasApiInternaAdapter` con `ConsultarContactoUseCase`.
  - Las cuentas no dependen de ningún módulo: los demás las consultan.
- **Solicitudes la usa:** su `ContactoPort` lo implementa `CuentasModuloAdapter`, que reemplaza la tabla de turistas de la demo (DN-15).
- **Pendiente:** crear y desactivar cuentas internas (RF-81) se hará con el módulo de administración. Ya existe el código `ESTABLECER_CONTRASENA`, y `POST /cuentas/contrasena` lo acepta.

## Estructura y despliegue

**DN-07. Estructura del servicio y despliegue en el piloto.**
- **Paquetes:** `co.edu.unimagdalena.samariantour.nucleo`, con un paquete por módulo (`cuentas`, `solicitudes`, `pagos`, `prestadores`, `administracion`, `indicadores` y `bandeja`) y `compartido`, cada módulo hexagonal como en el catálogo. Cada `package-info.java` dice qué requisitos le tocan. Las 9 primeras reglas de `ArquitecturaTest` son las del catálogo, con JPA, Hibernate y PostgreSQL en lugar de MongoDB; la 10 y la 11 cuidan el `api` de cada módulo (DN-17).
- **API:** prefijo `/api/nucleo` y errores en RFC 7807. El contrato es `contratos/nucleo.openapi.yaml`; `ContratoNucleoIT` falla si una ruta está en un lado y no en el otro, o si un esquema de respuesta no tiene los mismos campos que su DTO. Un cuerpo que no es JSON válido o un parámetro con el tipo equivocado responde 400 `DATO_INVALIDO` en cualquier módulo (`ManejadorPeticionIlegible`).
- **Docker Compose:** PostgreSQL 17 (`postgres-data`, sin puertos publicados), Mailpit (DN-30) y el núcleo con 2 réplicas de 512 MB, healthcheck y el perfil `demo`. El núcleo exige `NUCLEO_CIFRADO_CLAVE` y `NUCLEO_JWT_CLAVE_PRIVADA` en `infra/.env` (`bash infra/generar-claves.sh`); sin ellas Compose no arranca. Nginx enruta `/api/nucleo/` y bloquea `/api/nucleo/internal/` y `/api/nucleo/actuator/`. El CI construye también la imagen del núcleo.
- **Métricas:** como el catálogo (DC-48), `/actuator/prometheus` con los umbrales de latencia de cada ruta, solo desde la red de Docker.

## Cambios pendientes en los documentos de diseño

1. **Diagrama de paquetes, núcleo:** agregar el módulo `bandeja` con `BandejaSalidaApi`, la tabla `bandeja_salida` y la tarea programada que la publica (DN-06). El diseño la nombra como `BandejaSalidaAdapter`; en el código es un módulo con su API, para que los demás módulos la usen sin depender de su infraestructura.
2. **Diagrama entidad-relación:** agregar `bandeja_salida` (DN-06) y `decisiones_moderacion` (DN-08).
3. **Diagrama de despliegue:** PostgreSQL 17 y las rutas de Nginx del núcleo (DN-07).
4. **Diagrama de paquetes, módulo de administración:** los puertos de salida de la moderación son `ModeracionCatalogoPort` (consulta al catálogo), `DecisionesModeracionRepositoryPort`, `AvisosModeracionPort` (la bandeja) y `NotificacionPort`; la decisión sobre una oferta lleva el número de versión (DN-08).
5. **Diagrama de paquetes, módulo 02 (solicitudes):**
   - `IdempotenciaPort` vive en el módulo de solicitudes: el ADR-04 lo pide para enviar y responder; generar la sesión de cobro (RF-46) lo agregará pagos con su propio puerto o con `EjecucionIdempotente` (DN-12);
   - se agregan `TransaccionPort` (DN-11), `NotificacionRepositoryPort` y `AvisoPlataformaPort` (DN-13), y la API `solicitudes.api` para pagos (DN-17);
   - `NotificationPort` es solo el envío por WhatsApp; las notificaciones se guardan antes en la transacción del cambio y las envía una tarea programada (DN-13);
   - `PrestadoresPort` y `ContactoPort` tienen adaptadores provisionales de la demo (DN-15).
6. **Diccionario de clases, SolicitudDisponibilidad:**
   - la asignación es estática (RF-32 del SRS v1.1): no hay `tomar`, `liberar`, el máximo de 5 tomadas ni la liberación a las 2 h;
   - los campos de RF-30 de cada tipo están en `CamposDelTipo`: `horaSalida`, `horaReserva`, `fechaSalida` y `numeroUnidades` (DN-09);
   - todavía no tiene `codigo` corto: la solicitud se identifica por su UUID, que es lo que RF-30 pide mostrar;
   - `ventanaVenceEn` es `venceEn`, el plazo vigente en cualquier estado abierto; el aparte es `aparteHasta` (DN-09).
7. **Diagrama entidad-relación:** agregar `solicitudes`, con las columnas de V4 y V6, `transiciones_solicitud`, `claves_idempotencia` y `notificaciones_solicitud` (V3).
8. **Diagrama de paquetes, módulo de cuentas:**
   - se agrega `ConsultarPoliticaDatosUseCase` (DN-28), porque un controlador no lee repositorios;
   - `EmailPort` y `NotificationPort` tienen adaptadores para Mailpit y para el WhatsApp simulado (DN-28, DN-30);
   - se agrega `AvisosCatalogoPort`, que deja `SUPRESION_TURISTA` en la bandeja (DN-31);
   - `CrearCuentaInternaUseCase` y `DesactivarCuentaInternaUseCase` quedan para el módulo de administración (RF-81). `AnonimizarUsuarioUseCase` es hoy la supresión del turista (DN-31).
9. **Diccionario de clases, cuentas:**
   - `CodigoUnUso` agrega `intentosFallidos` (DN-28);
   - el token de renovación y el enlace del correo son `TokenOpaco` (`<id>.<secreto>`); `Sesion` guarda el hash del secreto (DN-29);
   - `Usuario` agrega `cambiarNombre`, `cambiarContrasena` y la anonimización, y `Turista`, `verificarWhatsapp`, `cambiarWhatsapp` y `suprimir`.
10. **Diagrama entidad-relación:** agregar `usuarios`, `politicas_datos`, `aceptaciones_politica`, `solicitudes_supresion`, `codigos_un_uso` y `sesiones` (V8).
11. **Diagrama de despliegue:** Mailpit en el piloto (DN-30).
