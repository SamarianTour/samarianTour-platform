# Decisiones del prototipo: núcleo transaccional

Registro de las decisiones tomadas para el servicio `nucleo` (módulos 1 Cuentas, 4 Solicitudes, 5 Pagos, 7 Prestadores, 8 Administración y 9 Indicadores, sobre PostgreSQL). Complementa el documento de diseño, el diagrama de paquetes y el diccionario de clases; donde los contradice, se indica en la sección final. Sigue el formato de `decisiones-catalogo-ofertas.md`.

- **Estado:** vigente, octubre de 2026. La base del servicio y la moderación (DN-08); cada módulo agrega sus decisiones con el número siguiente.
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

## Persistencia

**DN-01. El esquema lo crea Flyway.** Las migraciones están en `servicios/nucleo/src/main/resources/db/migration` y se aplican al arrancar. Hibernate solo valida que las entidades coincidan con el esquema (`ddl-auto: validate`): nunca crea ni cambia tablas.
- **Nombres:** `V<n>__<descripcion_en_minusculas>.sql`, con el número siguiente al último en `develop`. Una migración ya fusionada no se edita: un cambio va en una migración nueva.
- **Varias réplicas:** las dos réplicas pueden arrancar a la vez. Flyway serializa las migraciones con un candado de PostgreSQL, así que cada una se aplica una sola vez.
- **V1** crea la bandeja de salida (DN-06) y **V2**, el historial de moderación (DN-08).

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

## Estructura y despliegue

**DN-07. Estructura del servicio y despliegue en el piloto.**
- **Paquetes:** `co.edu.unimagdalena.samariantour.nucleo`, con un paquete por módulo (`cuentas`, `solicitudes`, `pagos`, `prestadores`, `administracion`, `indicadores` y `bandeja`) y `compartido`, cada módulo hexagonal como en el catálogo. Cada `package-info.java` dice qué requisitos le tocan. Las 9 reglas de `ArquitecturaTest` son las del catálogo, con JPA, Hibernate y PostgreSQL en lugar de MongoDB.
- **API:** prefijo `/api/nucleo` y errores en RFC 7807. El contrato es `contratos/nucleo.openapi.yaml`; `ContratoNucleoIT` falla si una ruta está en un lado y no en el otro, o si un esquema de respuesta no tiene los mismos campos que su DTO. Un cuerpo que no es JSON válido o un parámetro con el tipo equivocado responde 400 `DATO_INVALIDO` en cualquier módulo (`ManejadorPeticionIlegible`).
- **Docker Compose:** PostgreSQL 17 (`postgres-data`, sin puertos publicados) y el núcleo con 2 réplicas de 512 MB, healthcheck y el perfil `demo`. Nginx enruta `/api/nucleo/` y bloquea `/api/nucleo/internal/` y `/api/nucleo/actuator/`. El CI construye también la imagen del núcleo.
- **Métricas:** como el catálogo (DC-48), `/actuator/prometheus` con los umbrales de latencia de cada ruta, solo desde la red de Docker.

## Cambios pendientes en los documentos de diseño

1. **Diagrama de paquetes, núcleo:** agregar el módulo `bandeja` con `BandejaSalidaApi`, la tabla `bandeja_salida` y la tarea programada que la publica (DN-06). El diseño la nombra como `BandejaSalidaAdapter`; en el código es un módulo con su API, para que los demás módulos la usen sin depender de su infraestructura.
2. **Diagrama entidad-relación:** agregar `bandeja_salida` (DN-06) y `decisiones_moderacion` (DN-08).
3. **Diagrama de despliegue:** PostgreSQL 17 y las rutas de Nginx del núcleo (DN-07).
4. **Diagrama de paquetes, módulo de administración:** los puertos de salida de la moderación son `ModeracionCatalogoPort` (consulta al catálogo), `DecisionesModeracionRepositoryPort`, `AvisosModeracionPort` (la bandeja) y `NotificacionPort`; la decisión sobre una oferta lleva el número de versión (DN-08).
