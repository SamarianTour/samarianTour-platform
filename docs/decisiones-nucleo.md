# Decisiones del prototipo: núcleo transaccional

Registro de las decisiones tomadas para el servicio `nucleo` (módulos 1 Cuentas, 4 Solicitudes, 5 Pagos, 7 Prestadores, 8 Administración y 9 Indicadores, sobre PostgreSQL). Complementa el documento de diseño, el diagrama de paquetes y el diccionario de clases; donde los contradice, se indica en la sección final. Sigue el formato de `decisiones-catalogo-ofertas.md`.

- **Estado:** vigente, octubre de 2026. La base del servicio, la moderación (DN-08) y las solicitudes de disponibilidad (DN-09 a DN-17); cada módulo agrega sus decisiones con el número siguiente.
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

## Persistencia

**DN-01. El esquema lo crea Flyway.** Las migraciones están en `servicios/nucleo/src/main/resources/db/migration` y se aplican al arrancar. Hibernate solo valida que las entidades coincidan con el esquema (`ddl-auto: validate`): nunca crea ni cambia tablas.
- **Nombres:** `V<n>__<descripcion_en_minusculas>.sql`, con el número siguiente al último en `develop`. Una migración ya fusionada no se edita: un cambio va en una migración nueva.
- **Varias réplicas:** las dos réplicas pueden arrancar a la vez. Flyway serializa las migraciones con un candado de PostgreSQL, así que cada una se aplica una sola vez.
- **V1** crea la bandeja de salida (DN-06); **V2**, el historial de moderación (DN-08), **V3**, las solicitudes con su historial, las claves de idempotencia y las notificaciones (DN-11 a DN-13); **V4**, los campos de cada tipo de oferta y el índice de no duplicado con la hora de la reserva (DN-09, DN-11); **V5**, el nombre `SOLICITUD_PAGADA` de las notificaciones ya guardadas (DN-13), y **V6**, el aparte del cupo (DN-09).

**DN-02. Spring Data JPA, con el dominio separado.** Las entidades JPA y los repositorios viven en `infraestructura/salida/persistencia` de cada módulo (regla 8 de `ArquitecturaTest`), y el adaptador convierte entre la entidad y el modelo del dominio, que no conoce JPA (regla 1). Es el mismo esquema que el catálogo con sus documentos de MongoDB. `open-in-view` está apagado: cada caso de uso carga lo que necesita.

**DN-03. Siempre PostgreSQL.** A diferencia del catálogo, el núcleo no tiene un perfil `memoria`: arrancarlo exige PostgreSQL, y en Docker Compose ya lo tiene. Así cada puerto de salida tiene un solo adaptador. Las pruebas unitarias de la aplicación usan dobles en memoria escritos en la prueba, y las de integración, un PostgreSQL de Testcontainers con la misma versión del piloto (`IntegracionPostgresBase`).

## Seguridad

**DN-04. Los cinco roles del SRS** (`compartido/seguridad/Rol`): `TURISTA`, `PRESTADOR`, `ADMINISTRADOR`, `OPERADOR_DISPONIBILIDAD` y `GESTOR_DESTINO`. El catálogo solo distingue los tres primeros. Los nombres son los que debe llevar el token.

**DN-05. Identidad provisional y autorización por prefijo, como el catálogo** (DC-01, DC-13, DC-15).
- Mientras no exista `libs/seguridad-jwt`, la identidad llega en los headers `X-Usuario-Id`, `X-Rol` y, para el prestador, `X-Tipo-Prestador`, solo con el perfil `demo` (`IdentidadPorHeaders`). El JWT la reemplaza con otra implementación de `ResolutorIdentidad`; lo demás no cambia.
- Cada prefijo exige un rol (`SeguridadConfig`): `/turista/**`, `/prestador/**`, `/admin/**`, `/operador/**` y `/gestor/**`. Sin identidad, 401 `NO_AUTENTICADO`; con otro rol, 403 `ACCESO_DENEGADO`.
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

**DN-15. Prestadores y contactos provisionales** [SUPUESTO]. El operador asignado y el WhatsApp de cada prestador son del módulo de prestadores, y el contacto del turista es del de cuentas, que todavía están vacíos.
- **Tabla de la demo:** `PrestadoresPort` y `ContactoPort` tienen adaptadores provisionales que leen `nucleo.demo` en `application-demo.yml`, con los identificadores de la demo del catálogo y de sus opiniones:
  - la agencia (`10000000-...-001`) y la guía (`20000000-...-002`) las atiende el operador `60000000-...-001`;
  - el hotel (`30000000-...-003`) y el restaurante (`40000000-...-004`), el `60000000-...-002`;
  - los turistas `90000000-...-001` a `...003` tienen un WhatsApp ficticio verificado.
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

## Estructura y despliegue

**DN-07. Estructura del servicio y despliegue en el piloto.**
- **Paquetes:** `co.edu.unimagdalena.samariantour.nucleo`, con un paquete por módulo (`cuentas`, `solicitudes`, `pagos`, `prestadores`, `administracion`, `indicadores` y `bandeja`) y `compartido`, cada módulo hexagonal como en el catálogo. Cada `package-info.java` dice qué requisitos le tocan. Las 9 primeras reglas de `ArquitecturaTest` son las del catálogo, con JPA, Hibernate y PostgreSQL en lugar de MongoDB; la 10 y la 11 cuidan el `api` de cada módulo (DN-17).
- **API:** prefijo `/api/nucleo` y errores en RFC 7807. El contrato es `contratos/nucleo.openapi.yaml`; `ContratoNucleoIT` falla si una ruta está en un lado y no en el otro, o si un esquema de respuesta no tiene los mismos campos que su DTO. Un cuerpo que no es JSON válido o un parámetro con el tipo equivocado responde 400 `DATO_INVALIDO` en cualquier módulo (`ManejadorPeticionIlegible`).
- **Docker Compose:** PostgreSQL 17 (`postgres-data`, sin puertos publicados) y el núcleo con 2 réplicas de 512 MB, healthcheck y el perfil `demo`. Nginx enruta `/api/nucleo/` y bloquea `/api/nucleo/internal/` y `/api/nucleo/actuator/`. El CI construye también la imagen del núcleo.
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
