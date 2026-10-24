# Decisiones del prototipo: núcleo transaccional

Registro de las decisiones tomadas para el servicio `nucleo` (módulos 1 Cuentas, 4 Solicitudes, 5 Pagos, 7 Prestadores, 8 Administración y 9 Indicadores, sobre PostgreSQL). Complementa el documento de diseño, el diagrama de paquetes y el diccionario de clases; donde los contradice, se indica en la sección final. Sigue el formato de `decisiones-catalogo-ofertas.md`.

- **Estado:** vigente, octubre de 2026. Por ahora solo la base del servicio; cada módulo agrega sus decisiones con el número siguiente.
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

## Persistencia

**DN-01. El esquema lo crea Flyway.** Las migraciones están en `servicios/nucleo/src/main/resources/db/migration` y se aplican al arrancar. Hibernate solo valida que las entidades coincidan con el esquema (`ddl-auto: validate`): nunca crea ni cambia tablas.
- **Nombres:** `V<n>__<descripcion_en_minusculas>.sql`, con el número siguiente al último en `develop`. Una migración ya fusionada no se edita: un cambio va en una migración nueva.
- **Varias réplicas:** las dos réplicas pueden arrancar a la vez. Flyway serializa las migraciones con un candado de PostgreSQL, así que cada una se aplica una sola vez.
- **V1** crea la bandeja de salida (DN-06).

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

## Estructura y despliegue

**DN-07. Estructura del servicio y despliegue en el piloto.**
- **Paquetes:** `co.edu.unimagdalena.samariantour.nucleo`, con un paquete por módulo (`cuentas`, `solicitudes`, `pagos`, `prestadores`, `administracion`, `indicadores` y `bandeja`) y `compartido`, cada módulo hexagonal como en el catálogo. Cada `package-info.java` dice qué requisitos le tocan. Las 9 reglas de `ArquitecturaTest` son las del catálogo, con JPA, Hibernate y PostgreSQL en lugar de MongoDB.
- **API:** prefijo `/api/nucleo` y errores en RFC 7807. El contrato es `contratos/nucleo.openapi.yaml`, todavía sin rutas; `ContratoNucleoIT` falla si una ruta está en un lado y no en el otro.
- **Docker Compose:** PostgreSQL 17 (`postgres-data`, sin puertos publicados) y el núcleo con 2 réplicas de 512 MB, healthcheck y el perfil `demo`. Nginx enruta `/api/nucleo/` y bloquea `/api/nucleo/internal/` y `/api/nucleo/actuator/`. El CI construye también la imagen del núcleo.
- **Métricas:** como el catálogo (DC-48), `/actuator/prometheus` con los umbrales de latencia de cada ruta, solo desde la red de Docker.

## Cambios pendientes en los documentos de diseño

1. **Diagrama de paquetes, núcleo:** agregar el módulo `bandeja` con `BandejaSalidaApi`, la tabla `bandeja_salida` y la tarea programada que la publica (DN-06). El diseño la nombra como `BandejaSalidaAdapter`; en el código es un módulo con su API, para que los demás módulos la usen sin depender de su infraestructura.
2. **Diagrama entidad-relación:** agregar `bandeja_salida` (DN-06).
3. **Diagrama de despliegue:** PostgreSQL 17 y las rutas de Nginx del núcleo (DN-07).
