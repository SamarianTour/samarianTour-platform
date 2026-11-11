# Frontend de Samarian Tour

Aplicación Angular contra la API real: el turista busca ofertas, ve su ficha con galería de fotos, mapa y opiniones, y opina sobre las que pagó; el prestador registra su RNT y su cuenta de cobro, arma su perfil público, publica con sus fotos, ve sus ofertas, las edita y las retira; el gestor del destino consulta indicadores agregados de la demanda, del embudo de conversión y de la distribución por tipo de prestador. Cada persona tiene su cuenta: se registra, inicia sesión, recupera su contraseña, consulta y corrige sus datos y puede eliminar su cuenta.

Angular 22 con componentes standalone, signals, formularios reactivos y carga diferida por funcionalidad. El mapa usa Leaflet con OpenStreetMap, sin API key.

## Requisitos

Node.js 24 (o 22.22 en adelante) y npm. El backend debe estar levantado en `http://localhost:8080` con el perfil `demo` (ver el [README principal](../README.md#levantar-el-piloto)).

## Comandos

```bash
npm ci             # instalar dependencias
npm start          # desarrollo en http://localhost:4200, con recarga al guardar
npm run build      # compilación de producción en dist/samarian-tour/browser
npm test           # pruebas unitarias con Vitest (en CI: npm test -- --no-watch)
npm run gen:api    # regenerar los tipos de la API desde los tres contratos
```

## Conexión con el backend

- Hay tres bases, siempre relativas al origen de la página: `/api/catalogo`, `/api/nucleo` (cuentas, solicitudes, pagos, administración) y `/api/recomendacion` (`core/api/api.config.ts`).
- En desarrollo, `ng serve` reenvía `/api` a `http://localhost:8080` (`proxy.conf.json`). En Docker Compose, el mismo Nginx sirve la aplicación y reenvía la API. En los dos casos el navegador habla con un solo origen, así que el backend no necesita CORS.

## Tipos generados desde el contrato

Los tipos de cada servicio se generan con `openapi-typescript` a partir de su contrato en `contratos/`:

| Archivo generado | Contrato |
|---|---|
| `src/app/core/api/catalogo-api.ts` | `catalogo.openapi.yaml` |
| `src/app/core/api/nucleo-api.ts` | `nucleo.openapi.yaml` |
| `src/app/core/api/recomendacion-api.ts` | `recomendacion.openapi.yaml` |

**No se editan a mano**: si cambia un contrato, se corre `npm run gen:api` (genera los tres; también hay `gen:api:catalogo`, `gen:api:nucleo` y `gen:api:recomendacion`) y se versiona el resultado. El job `frontend` de CI regenera los tipos y hoy verifica que `catalogo-api.ts` quede igual al versionado.

`src/app/core/api/modelos.ts` (catálogo) y `modelos-nucleo.ts` (núcleo) solo les ponen nombres cortos a esos tipos. `CodigoError` reúne los códigos de error de los tres servicios. Las tablas de textos de `src/app/shared/etiquetas.ts` y `MENSAJES` (`core/errores/error-api.ts`) exigen todos los valores de cada enumeración y cada código, así que un valor nuevo en un contrato rompe el build hasta ponerle nombre.

Los archivos generados están versionados para que la imagen de Docker se construya solo con la carpeta `frontend/`, sin los contratos.

## Estructura

```
src/app
├── core/                    lo que existe una sola vez en la aplicación
│   ├── api/                 tipos generados, servicios HTTP del catálogo, del prestador, de opiniones, de cuentas, de solicitudes, del operador, de pagos y del administrador (núcleo y catálogo) y de los indicadores
│   ├── tiempo/              reloj que se renueva solo, para los plazos
│   ├── navegacion/          salir de la aplicación hacia otra dirección (la pasarela de pago)
│   ├── errores/             interceptor que traduce el problem+json a un mensaje para el usuario
│   ├── sesion/              sesión con token, interceptor, guards por rol, rutas y enlaces por rol
│   └── avisos/              mensajes breves de confirmación
├── shared/                  piezas reutilizables: mapa, galería de fotos, menú de la cuenta, marco de las pantallas de acceso, "próximamente", iconos, estrellas, paginación, diálogo de confirmación, pasos de la solicitud, pipes y textos
└── features/
    ├── turista/             inicio (/), catálogo (/catalogo), paquetes (/paquetes), ficha de zona (/zonas/:id), ficha con opiniones (/ofertas/:id), mis opiniones (/mis-opiniones) y las solicitudes de disponibilidad (solicitudes/)
    ├── prestador/           panel con menú lateral: inicio (dashboard), mis ofertas, detalle y formulario de publicar o editar, RNT y tipo, perfil público, WhatsApp de contacto y cuenta de cobro
    ├── operador/            panel con menú lateral, cola, historial, atender una solicitud y avisos
    ├── admin/               panel con menú lateral: prestadores y RNT, moderación, cuentas internas, comisión y pagos tardíos, zonas, alertas de sesgo y pendientes técnicos
    ├── gestor/              panel con menú lateral y el panel de indicadores: Demanda, Embudo de conversión y Distribución por tipo de prestador, con el filtro de periodo, gráficos de barras y sus tablas de datos
    ├── recomendacion/       asistente de recomendaciones, página de alertas del destino y las secciones de alertas y alternativas de la ficha (públicas)
    └── cuentas/             iniciar sesión, registro, recuperar la contraseña, mis datos y "no tienes permiso"
```

| Ruta | Quién | Pantalla |
|---|---|---|
| `/` | Todos | P01.0 Inicio: buscador, cuatro tarjetas de tipo, zonas reales y la invitación a las recomendaciones. Una raíz con filtros del catálogo (`/?tipo=...`) redirige a `/catalogo` con los mismos parámetros |
| `/catalogo` | Todos | Catálogo con búsqueda por texto (2 a 100 caracteres) y filtros en los query params, y paginación de 12; cada tarjeta muestra la miniatura de la portada |
| `/paquetes` | Todos | P01.3 Catálogo de paquetes: el mismo catálogo con el tipo fijo en `PAQUETE` y su propio título |
| `/zonas/:id` | Todos | Ficha de la zona: descripción, marcas (área protegida, alta afluencia), buenas prácticas, condiciones de visita y alertas. Se llega desde el Inicio y desde la ficha de una oferta |
| `/ofertas/:id` | Todos | Ficha con galería de fotos, zona, perfil del prestador, mapa y opiniones; el turista con sesión puede "Dejar una opinión". Trae las alertas de su zona (P08.2) y, si la zona es de alta afluencia, alternativas con menos visitantes (P08.3) |
| `/recomendaciones` | Todos | P08.1 Recomendaciones: asistente de viaje que sugiere de 3 a 5 ofertas explicadas |
| `/alertas` | Todos | P08.2 Alertas del destino por zona, con su fuente y la hora de actualización. No está en el menú: se llega desde Recomendaciones y desde la ficha |
| `/iniciar-sesion` | Todos | P06.2 Iniciar sesión (`?volver=` lleva a la página que se quería ver) |
| `/registro` | Todos | P06.1 Registro de turista o prestador (`?tipo=prestador` deja elegido el prestador) |
| `/recuperar-contrasena` | Todos | P06.3 Pedir el enlace para elegir una contraseña nueva |
| `/restablecer-contrasena?token=…` | Todos | Poner la contraseña nueva; es el enlace que envía el correo del núcleo |
| `/mis-datos` | Con sesión | P06.4 Mis datos y derechos, verificar el WhatsApp y eliminar la cuenta |
| `/sin-permiso` | Con sesión | "No tienes permiso" cuando se abre la sección de otro rol |
| `/mis-opiniones` | Turista | Opiniones del turista, con su estado y el motivo si fue rechazada |
| `/turista/solicitar/:ofertaId` | Turista | P02.1 Formulario de solicitud de disponibilidad (el botón de la ficha lleva aquí) |
| `/turista/solicitudes` | Turista | P02.2 Mis solicitudes: los 9 estados, filtros Todas / Activas / Finalizadas y plazos |
| `/turista/solicitudes/:id` | Turista | P02.2 Detalle: estado, historial, plazo y condiciones; P02.3 confirmar o declinar si está atendida; P04.1 pagar, "Confirmando tu pago" y P04.2 intentos y reintento. El núcleo y la pasarela enlazan aquí (regreso del pago, aviso de WhatsApp) |
| `/prestador` | Prestador | P03.0 Inicio: estado del registro y del RNT, ofertas por estado y accesos a cada sección |
| `/prestador/rnt` | Prestador | P03.1 RNT y tipo: registrar el RNT, ver la validación, cambiar el número y el tipo |
| `/prestador/perfil` | Prestador | P03.2 Perfil público y hasta 6 fotos |
| `/prestador/whatsapp` | Prestador | P03.4 WhatsApp de contacto: configurar el número y verificarlo con el código |
| `/prestador/cuenta-cobro` | Prestador | P03.5 Cuenta de cobro: dónde recibe lo que le corresponde por cada pago |
| `/prestador/ofertas` | Prestador | Mis ofertas, con su estado y las acciones |
| `/prestador/ofertas/nueva` | Prestador | Publicar una oferta, con sus fotos |
| `/prestador/ofertas/:id` | Prestador | Detalle: versión publicada y versión pendiente, con su contenido completo (`GET /prestador/ofertas/{id}`, DC-50) |
| `/prestador/ofertas/:id/editar` | Prestador | Editar una oferta, partiendo de su versión pendiente (en moderación o rechazada, con su motivo) si existe y si no de la publicada |
| `/operador` | Operador | P02.4 Cola: las solicitudes pendientes, las urgentes (6 h o menos) primero y resaltadas, con paginación |
| `/operador/historial?estado=` | Operador | Historial: la misma consulta de la cola con un estado, de la más reciente a la más antigua |
| `/operador/solicitudes/:id` | Operador | P02.5 Atender: contactar al prestador por WhatsApp y registrar "hay cupo" (con precio total y condiciones) o "no hay cupo" |
| `/operador/avisos` | Operador | Avisos de solicitudes nuevas, del más reciente al más antiguo |
| `/gestor?desde=&hasta=` | Gestor | P09.1 Panel de indicadores, vista Demanda: solicitudes por zona y tipo de oferta |
| `/gestor/embudo?desde=&hasta=` | Gestor | P09.1 Embudo de conversión: de las enviadas, cuántas se atienden, se confirman y se pagan, en total y por tipo de oferta |
| `/gestor/distribucion?desde=&hasta=` | Gestor | P09.1 Distribución por tipo de prestador: solicitudes y pagos por categoría de prestador |
| `/admin` | Administrador | P05.1 Prestadores y RNT: verificar o sincronizar el RNT, asignar operador |
| `/admin/moderacion` | Administrador | P05.2 Moderación de ofertas y de opiniones |
| `/admin/cuentas` | Administrador | P05.3 Cuentas internas: crear operadores y gestores, desactivar y reactivar |
| `/admin/comision` | Administrador | P05.4 Comisión vigente, historial y cambio; pagos tardíos |
| `/admin/zonas?estado=` | Administrador | P05.5 Zonas: registrar, editar, inactivar y reactivar |
| `/admin/alertas-sesgo` | Administrador | Alertas de sesgo |
| `/admin/pendientes` | Administrador | Pendientes técnicos: avisos al catálogo rechazados y eventos de pago fallidos |

## Solicitudes de disponibilidad

El turista pide disponibilidad sobre una oferta (RF-30 a RF-34), el operador la atiende y el turista confirma o declina las condiciones (RF-43). Lo que cubre esta parte del flujo, en `features/turista/solicitudes`:

- **Formulario** (`/turista/solicitar/:ofertaId`). Pide solo lo que el contrato necesita y cambia con el tipo de oferta: actividad y paquete, la hora de salida (una de las de la oferta); alojamiento, la fecha de salida y las unidades; restaurante, la hora de la reserva. El nombre y el contacto salen de la cuenta. Con una sola hora de salida y las personas en 1 solo hay que elegir la fecha (QAS-08). Sin sesión, el botón de la ficha lleva a iniciar sesión y vuelve; sin el WhatsApp verificado no se envía.
- **Mis solicitudes y detalle.** Cada uno de los 9 estados tiene su etiqueta, color y explicación (`estado-solicitud.ts`). Los plazos se cuentan en vivo (`plazo.ts`, `core/tiempo/reloj.ts`). Se vuelve a consultar el estado real al regresar a la pestaña, al reconectarse y cuando un plazo termina (`core/api/reconsulta.ts`, RF-33).
- **Confirmar o declinar** (`responder-condiciones`). Solo en una solicitud atendida y dentro del plazo. Declinar pide confirmación. Si el núcleo dice que algo cambió (plazo vencido, estado distinto), se explica y se consulta de nuevo.
- **Idempotencia.** Enviar y responder llevan una `Idempotency-Key` (ADR-04). `ClaveDeIdempotencia` repite la clave mientras el cuerpo no cambie, y la renueva si el usuario corrige algo, para no recibir un 422.
- **Fechas.** La primera fecha posible es dos días calendario después de hoy **en la hora de Colombia**, no la del navegador.
- **El pago** de una solicitud confirmada o con el pago fallido está en la sección "Pagos".

La lógica (estados, plazos, validaciones y armado de la petición) está en funciones puras con pruebas propias.

## Panel del operador

El operador de disponibilidad verifica el cupo con el prestador y registra el resultado (RF-32 a RF-40). Está en `features/operador`, dentro de un panel con menú lateral (Cola, Historial y Avisos) que solo abre la sesión de un operador. El servicio es `core/api/operador.service.ts`; solo ve las solicitudes de los prestadores que tiene asignados.

- **Cola** (`/operador`, P02.4). Las pendientes, con la urgencia de cada una (`cola.ts`): *urgente* con 6 horas o menos para atenderla (RF-36), *en plazo*, o *vencida* si el plazo ya terminó. Van primero las urgentes, luego las que están en plazo y al final las vencidas; dentro de cada grupo, la que vence antes. La urgencia se lee de `proximaAVencer` y también del reloj de la pantalla, para que una solicitud que se vuelve urgente con la pantalla abierta se resalte sin recargar. Las urgentes no dependen solo del color: llevan el texto "Urgente", un icono y una barra a la izquierda.
- **Historial** (`/operador/historial?estado=`). La API usa la misma ruta de la cola: sin `estado` devuelve las pendientes y con otro estado, el historial. Por eso el historial se consulta estado por estado, con un enlace por cada uno de los ocho estados distintos de Pendiente; no existe una vista "todos". El estado y la página viajan en la dirección. Un estado desconocido, o `PENDIENTE`, abre las atendidas en lugar de llegar a la API.
- **Atender** (`/operador/solicitudes/:id`, P02.5). Muestra la solicitud, un enlace de WhatsApp al prestador (`wa.me`, en otra pestaña) y el historial, y pide el resultado:
  - *Hay cupo*: **precio total** en pesos (un solo campo, el contrato pide el total y no el precio por persona), política de cancelación, la casilla *"Confirmé con el prestador que el cupo queda apartado durante el plazo"* (RF-38, sin ella no se envía) y otras condiciones opcionales.
  - *No hay cupo*: un motivo opcional de hasta 500 caracteres (RF-40).
  - **Referencia junto al precio.** Se muestra lo que pidieron (personas; o noches, unidades y huéspedes en alojamiento), el precio de la oferta en el catálogo y un cálculo de referencia. Es solo informativo: el campo de precio nace vacío y el total lo escribe el operador. Si el catálogo no responde, se avisa y se puede seguir.
  - **Errores del núcleo.** `SIN_TIEMPO_PARA_CONFIRMAR` deja solo "no hay cupo" (RF-39); `TRANSICION_INVALIDA` y `PLAZO_VENCIDO` vuelven a consultar la solicitud y muestran su estado real, ya sin formulario. Una solicitud que ya no está pendiente se muestra como consulta, con lo que se registró (una vez atendida, sus datos no cambian).
- **Avisos** (`/operador/avisos`, RF-34). Cada solicitud nueva deja un aviso `NUEVA_SOLICITUD`, que lleva a atenderla.
- **Actualización.** Cola, historial y avisos vuelven a consultar al regresar a la pestaña y al reconectarse (`core/api/reconsulta.ts`), para ver las solicitudes nuevas sin recargar.

### Decisiones de diseño

- **El operador no ve datos personales del turista.** Ni la cola, ni atender, ni los avisos muestran su nombre, su correo o su WhatsApp: el operador gestiona con el prestador y no los necesita (minimización de datos, Ley 1581). La API tampoco los entrega. Por eso la tabla del diseño de referencia no tiene la columna "Turista". El único dato que viene del turista es el comentario que escribió en la solicitud, porque el operador debe transmitirlo al prestador.
- **No existe "el prestador no respondió".** El diseño de referencia ofrecía esa opción, pero el contrato solo deja registrar *Atendida* o *No disponible*; `SIN_RESPUESTA` lo pone el sistema cuando se vence el plazo.
- **Sin cifras de "En revisión" ni "Resueltas hoy".** La API no las entrega; la cola muestra solo los pendientes, con el total que informa la API.
- **Cada pantalla tiene su propio `.css`.** Las clases del diseño de referencia (tabla, fila urgente, tarjeta de cifra, opciones del resultado) están en el `.css` de la pantalla que las usa; la tabla de solicitudes, que comparten la cola y el historial, es un componente aparte (`tabla-solicitudes`).

### Para el equipo de backend

- **Claves de `datos` en los avisos.** El contrato describe `datos` como un mapa de texto sin fijar sus claves. El frontend usa las del ejemplo de `NUEVA_SOLICITUD` (`nombreOferta`, `fechaInicio`, `horaSalida` o `horaReserva`, `personas`) y omite lo que falte. Conviene fijarlas en el contrato.
- **Historial sin "todos" (opcional).** Hoy no hay una consulta que devuelva todas las solicitudes tramitadas de una vez; el frontend las ofrece estado por estado. Si el equipo quiere una vista "todos", haría falta un valor o un parámetro nuevo en `GET /operador/solicitudes`.

## Panel del administrador

El administrador gestiona la plataforma desde `/admin`, dentro de un panel con menú lateral (`shared/panel-lateral`) que solo abre la sesión de un administrador. Está en `features/admin`; los servicios son `core/api/admin.service.ts` (núcleo, `/api/nucleo/admin/**`) y `core/api/admin-catalogo.service.ts` (catálogo, `/api/catalogo/admin/**`). Cada pantalla tiene su lógica en funciones puras con pruebas propias (`prestadores.ts`, `moderacion.ts`, `cuentas.ts`, `comision.ts`, `pagos-tardios.ts`, `zonas.ts`, `alertas-sesgo.ts`, `pendientes.ts`).

- **Prestadores y RNT** (`/admin`, P05.1). Lista de prestadores con su estado de RNT. Sincronizar con el RNT puede tardar: el botón queda deshabilitado con "Sincronizando…" mientras responde y el frontend **no pone ningún tiempo máximo** a esa petición. Al asignar operador se muestra el nombre de cada operador (se lee de las cuentas internas), no su id.
- **Moderación** (`/admin/moderacion`, P05.2). Las colas de ofertas y opiniones se leen en el catálogo; la decisión se envía al núcleo y el catálogo la aplica por la bandeja de salida en unos segundos, así que el cambio puede no verse al instante.
- **Cuentas internas** (`/admin/cuentas`, P05.3). Se ven todas, pero solo se pueden crear operadores y gestores; las cuentas de administrador son de solo consulta. Desactivar pide confirmación y explica que las sesiones abiertas duran a lo sumo 15 minutos.
- **Comisión y pagos tardíos** (`/admin/comision`, P05.4). Comisión vigente, historial y cambio (de 0 a menos de 100, con hasta 2 decimales; no es retroactiva), y la lista de pagos tardíos.
- **Zonas** (`/admin/zonas`, P05.5). Se escriben directo en el catálogo. Una zona nunca se elimina: se inactiva, sale de la lista pública y no admite ofertas nuevas ni ediciones, pero sus ofertas siguen visibles. Como `PUT` reemplaza todos los datos, inactivar y reactivar mandan la zona completa con solo `activa` cambiada. El nombre es único sin importar mayúsculas, tildes ni espacios (`ZONA_DUPLICADA`).
- **Alertas de sesgo** (`/admin/alertas-sesgo`). Cada día se compara el porcentaje de apariciones de cada prestador en las recomendaciones con su porcentaje de las ofertas visibles; si el índice pasa de 1,5 se abre una alerta, y se cierra sola cuando baja. La pantalla solo las consulta.
- **Pendientes técnicos** (`/admin/pendientes`). Dos listas: los avisos al catálogo rechazados, con reenvío, y los eventos de pago fallidos, con reintento.

### Para el equipo de backend

- **Tiempo máximo de Nginx en la sincronización del RNT.** El frontend espera la respuesta sin límite, pero `/api/nucleo/` pasa por Nginx con el `proxy_read_timeout` por defecto (60 s). Si el RNT tarda más, el corte vendrá de la infraestructura. Conviene subirlo para esa ruta o hacer la sincronización asíncrona.
- **Textos libres.** El `tipo` de un aviso y el estado que informa la pasarela en un evento fallido no son una lista cerrada en el contrato; el frontend los muestra tal como llegan. Conviene fijarlos.
- **Motivo del rechazo de un intento de pago.** Ver "Pagos": `IntentoPago` no trae el motivo.

## Panel del prestador

El prestador gestiona su registro y sus ofertas desde `/prestador`, dentro de un panel con menú lateral (Inicio, Mis ofertas, Publicar oferta, Mi RNT, Perfil público, WhatsApp de contacto y Cuenta de cobro) que solo abre la sesión de un prestador. Está en `features/prestador`. El núcleo guarda su registro, su WhatsApp y su cuenta de cobro (`core/api/prestador.service.ts`, `/api/nucleo/prestador/**`); el catálogo, su perfil público, sus fotos y sus ofertas (`perfil-prestador.service.ts` y `ofertas-prestador.service.ts`, `/api/catalogo/prestador/**`). Cada pantalla tiene su lógica en funciones puras con pruebas propias (`inicio.ts`, `rnt.ts`, `whatsapp.ts`, `cuenta-cobro.ts`, `perfil.ts`, `estado-oferta.ts`).

- **Inicio** (`/prestador`, P03.0). Muestra el estado del RNT, el número, la razón social, el tipo, la última sincronización y si el WhatsApp está verificado, cuántas ofertas hay en cada estado y accesos a las otras secciones. Si el RNT está inactivo, no se encuentra o no se ha registrado, lo explica con claridad —sus ofertas no se muestran en el catálogo— y enlaza a la sección RNT; con la verificación pendiente avisa con calma que no hay nada que corregir. El registro y las ofertas se piden por separado: si una consulta falla, la otra igual se ve.
- **RNT y tipo** (`/prestador/rnt`, P03.1). Sin registro pide el número y el tipo (`POST /prestador/registro`); con registro muestra cómo quedó la validación y deja cambiar el número (`PUT /prestador/rnt`) y el tipo (`PUT /prestador/tipo`). Un guía independiente declara modalidades e idiomas y no empleados; los demás tipos, el número de empleados. La validación contra el registro oficial puede tardar unos segundos: el botón queda deshabilitado con "Validando…" y la petición no tiene tiempo máximo. **Tras registrar y tras cambiar el tipo se renueva la sesión**, porque el tipo viaja en el token; si la renovación falla, la pantalla lo explica. Cambiar solo el número no la renueva.
- **Perfil público** (`/prestador/perfil`, P03.2). Nombre comercial, descripción (la única obligatoria), horario, sitio web y redes sociales (una dirección `http` o `https` por línea). Se publica de inmediato, sin moderación. Guardar reemplaza los datos, así que lo que se deja en blanco queda vacío; las fotos no viajan con él.
- **WhatsApp de contacto** (`/prestador/whatsapp`, P03.4). Un número nuevo queda sin verificar y el núcleo envía un código de 6 dígitos; mientras no se verifique, el prestador no recibe el aviso de una solicitud pagada (RF-57). El número nunca es público.
- **Cuenta de cobro** (`/prestador/cuenta-cobro`, P03.5). Cuenta de ahorros, corriente, Nequi o Daviplata. Las bancarias piden el banco; las billeteras, un celular de 10 dígitos que empieza por 3. Se registra una vez; si cambia, la anterior queda en el historial del núcleo. La comisión se descuenta de lo que recibe el prestador y nunca se suma al turista.
- **Editar y detalle** (`/prestador/ofertas/:id` y `/editar`, DC-50). Usan `GET /prestador/ofertas/{id}`, que trae el contenido completo de la versión publicada y de la pendiente.

### Decisiones de diseño

- **Editar parte de la versión pendiente si existe.** Guardar crea una versión nueva que reemplaza a la pendiente y la publicada sigue visible hasta que se apruebe la nueva. Por eso el formulario parte de la pendiente —en moderación o rechazada— y si no, de la publicada. Una versión rechazada muestra su motivo arriba del formulario y el detalle ofrece "Corregir la versión rechazada". También se puede editar una oferta que nunca se aprobó; solo una retirada no se edita (el retiro es definitivo).
- **El número de la cuenta de cobro solo se escribe.** El núcleo lo guarda cifrado (RNF-08) y devuelve solo los últimos 4 dígitos, que es lo único que se muestra. El campo lleva `autocomplete="off"`, no se corrige ni se guarda en el navegador (una prueba revisa `localStorage` y `sessionStorage`), se vacía al guardar y ningún mensaje lo repite: los errores de la API sobre un campo de la cuenta se reemplazan por un texto fijo, porque su detalle podría traer el valor. Para cambiar cualquier dato de la cuenta hay que volver a escribir el número.
- **La verificación del WhatsApp es el mismo componente del turista.** `VerificarWhatsapp` acepta, de forma opcional, el número, el aviso y cómo verificar y pedir otro código; sin ellas se comporta como en Mis datos. En el prestador no toca la cuenta de la sesión.
- **Las fotos del perfil usan el selector de las ofertas.** `SelectorFotos` recibe cómo subir y quitar, los textos ("del perfil"), si hay orden y si es obligatorio. En el perfil cada foto se sube y se quita en el servidor al instante, sin orden ni portada (la API no los tiene) y el perfil puede quedarse sin fotos. Hay que crear el perfil antes de subir fotos.
- **Lo que el diseño de referencia traía y no se construyó** porque el contrato no lo tiene: las cifras de vistas, calificación y solicitudes recientes del dashboard; el certificado del RNT en PDF; la persona responsable, el logo y las zonas del perfil; el teléfono fijo, el correo, el horario y el mensaje automático del canal de contacto (el horario de atención va en el perfil); y el documento del titular de la cuenta de cobro.

### Para el equipo de backend

- **Dashboard.** No hay datos de actividad del prestador (vistas de sus ofertas, calificación promedio, solicitudes recientes). Si el equipo los quiere mostrar, haría falta un endpoint nuevo.
- **Perfil público.** El contrato solo tiene nombre comercial, descripción, horario, sitio web, redes y fotos. Persona responsable, logo y zonas donde opera, que traía el diseño, no existen. Las fotos del perfil no se pueden reordenar.
- **Cuenta de cobro.** El detalle de un `DATO_INVALIDO` sobre `numero` no debería repetir el número recibido (el frontend ya no lo muestra, pero queda en la respuesta y puede acabar en un registro). `CuentaCobro.entidad` es obligatorio en la respuesta: para Nequi y Daviplata el frontend no lo muestra, pero conviene confirmar qué devuelve el núcleo. El historial de cuentas anteriores no se puede consultar.
- **Canal de contacto.** No hay teléfono fijo, correo, horario ni mensaje automático del prestador, que el diseño de referencia mostraba.

## Panel del gestor

El gestor del destino consulta indicadores agregados del turismo en Santa Marta (RF-85 a RF-89, P09.1). Está en `features/gestor`, dentro del panel lateral compartido (`shared/panel-lateral`, el mismo del administrador) con una sola sección: "Panel de indicadores". El servicio es `core/api/indicadores.service.ts`: tres `GET` de solo lectura —`/api/nucleo/gestor/indicadores/tablero`, `/embudo` y `/categorias`— que aceptan los mismos dos filtros opcionales, `desde` y `hasta`; el contrato no ofrece otros, así que no hay filtro por zona ni por tipo. La lógica de cálculo y de formato (`periodo.ts`, `formato.ts`, `demanda.ts`, `embudo.ts`, `distribucion.ts`) está en funciones puras con pruebas propias.

- **Periodo** (el encabezado, común a las tres vistas). Atajos —últimos 7, 30 y 90 días, este mes y mes anterior— o dos fechas a mano. Los días se cuentan en **hora de Colombia**, como hace el núcleo, no en la del navegador. Se valida antes de enviar: las dos fechas, inicio no posterior al final, final no posterior a hoy y como máximo 366 días. El periodo viaja en la dirección (`?desde=AAAA-MM-DD&hasta=AAAA-MM-DD`), así se puede compartir y se conserva al cambiar de vista; sin él se consultan los últimos 30 días, igual que el núcleo. Una dirección con un periodo inválido o a medias no rompe la pantalla: usa el de por defecto.
- **Demanda** (`/gestor`). Cifras (total de solicitudes, zonas y tipos de oferta con solicitudes) y barras por zona y tipo de oferta, de mayor a menor. El tablero trae solo el `zonaId`; el nombre y las marcas "Área protegida" y "Alta afluencia" salen de la lista pública de zonas.
- **Embudo de conversión** (`/gestor/embudo`). Las cuatro etapas (enviadas, atendidas, confirmadas, pagadas) con su cantidad, su porcentaje sobre las enviadas y qué parte de la etapa anterior llegó a ella, y el mismo embudo separado por tipo de oferta.
- **Distribución por tipo de prestador** (`/gestor/distribucion`). Cómo se reparte la demanda entre prestadores grandes, medianos, pequeños y guías independientes, según la categoría que tenía el prestador al recibir la solicitud, y cuántas de esas solicitudes se pagaron (porcentaje de pago).

### Por qué aparece «menos de 3» (RET-03)

Los indicadores son **datos agregados**. Para proteger la privacidad (RET-03, DN-41), cuando una cifra es de 1 o 2 solicitudes el núcleo **no la entrega** —ni su conteo ni su porcentaje—: así nadie puede identificar a un turista o a un prestador con una consulta de un periodo corto o de un tipo poco frecuente. La pantalla lo explica con una nota bajo las pestañas y lo trata así:

- Un conteo omitido se escribe **«menos de 3»** (*fewer than 3*), la barra va **rayada** y su porcentaje dice **«sin dato»**. Un cero es un cero y se muestra como tal: no es lo mismo que «menos de 3».
- **Nunca se estima ni se suma.** Los totales son los que entrega el núcleo, no la suma de las filas (las de 1 o 2 no traen su conteo y la suma saldría de menos). Las conversiones calculadas aquí —de una etapa a la siguiente y el porcentaje de pago— solo se calculan cuando las dos cantidades se conocen y la anterior no es cero; si alguna falta, queda «sin dato».
- Un total omitido (1 o 2 solicitudes en el periodo) no se confunde con un periodo sin solicitudes: en el primer caso se dibujan los gráficos con sus barras rayadas; en el segundo, un mensaje.

### Gráficos accesibles

Los gráficos son barras horizontales hechas con HTML y CSS (`componentes/grafico-barras`), **sin librerías**: bastan para lo que se muestra, una librería de gráficos suma peso al paquete inicial (ver "Tamaño de la aplicación"), y los gráficos de `canvas` son poco accesibles. Están pensados para WCAG 2.1 AA:

- Cada gráfico es **una sola imagen para los lectores de pantalla** (`role="img"`) con un resumen que lee los datos, y trae su **tabla de datos** en un desplegable "Ver los datos en una tabla" (`componentes/tabla-datos`), visible para todos, con título, encabezados de columna y de fila.
- El **valor de cada barra está escrito** junto a ella: el largo y el color no son lo único que informa. Una barra de conteo omitido se distingue además por su trama rayada.
- El color de las barras es `#0b7373`, el mismo mínimo que se usa para texto blanco; las animaciones solo corren si la persona no pidió menos movimiento. En celular las cifras pasan a una columna y las tablas se desplazan sin romper la página.

### Decisiones de diseño

- **Las vistas son rutas**, no pestañas `role="tab"`: cada una es una página con su dirección y su propia consulta. Son enlaces con `aria-current` y conservan el periodo.
- **Los nombres de las vistas** siguen el documento de diseño (Entregable 2, Tabla 3): Demanda, Embudo de conversión y Distribución por tipo de prestador. La dirección `/gestor` es la de Demanda.
- **Las categorías van de las más grandes a las más pequeñas** (gran empresa, mediana, micro o pequeña, guía independiente) y "sin categoría" al final, no en el orden del núcleo. Sus nombres son los del panel del prestador.
- **Dos gráficos de barras en la distribución, no una barra apilada.** Una barra apilada necesita una leyenda de colores y el color no debe ser lo único que informa.
- **Sin "Exportar".** El diseño de referencia traía el botón, pero el contrato no tiene ninguna ruta de exportación.
- **Lo que el diseño de referencia traía y no se construyó** porque el contrato no lo tiene: las variaciones frente al periodo anterior (+5 %), las solicitudes por mes, las visitas por zona y la distribución de tipos de oferta del catálogo.

### Para el equipo de backend

- **Nombre de la zona.** El tablero trae solo `zonaId`. El frontend busca el nombre en `GET /zonas`, que solo devuelve las zonas **activas**: una zona inactiva con solicitudes en el periodo se muestra como "Zona sin nombre (id)". Conviene que `DemandaZona` traiga también el nombre de la zona, o que el gestor pueda leer las zonas inactivas.
- **Series y comparaciones.** No hay solicitudes por mes ni variación frente al periodo anterior, que el diseño de referencia mostraba. Si se quieren, haría falta un endpoint nuevo (o un parámetro de agrupación).
- **Visitas.** No hay datos de visitas a las ofertas ni a las zonas.
- **Exportación.** No hay ruta para descargar los indicadores.
- **Fecha final en el futuro.** El contrato no dice qué pasa; el frontend no la deja enviar.

## Pagos

El turista paga una solicitud confirmada (RF-46 a RF-52) en la pasarela externa, que puede ser la **simulada** del núcleo (con los botones "Aprobar" y "Rechazar") o el sandbox de **Wompi**; ver "Pasarela de pagos" en el README principal. La pantalla es el detalle de la solicitud (`/turista/solicitudes/:id`): el núcleo y la pasarela devuelven al turista exactamente ahí. El código está en `features/turista/solicitudes` (`pagar-solicitud`, `confirmando-pago`, `pago-confirmado`, `intentos-pago` y la lógica pura de `pagos.ts`) y en `core/api/pagos.service.ts`.

### Decisiones de diseño

- **En la plataforma no se piden datos de tarjeta ni de PSE: el cobro ocurre solo en la pasarela.** El diseño de referencia (`pago.html`) traía un formulario de tarjeta y un selector de método de pago; no se construyó. La pantalla de pago es un resumen, el aviso de que se paga en una pasarela externa y el botón "Pagar", que pide la sesión de cobro (`POST /turista/solicitudes/{id}/pago`, sin cuerpo: no viaja ningún dato de pago, RF-47) y manda al turista a `checkout.url`. El método (tarjeta, PSE, Nequi) se elige allí. Una prueba comprueba que el bloque de pago no tiene ningún campo.
- **El estado del pago lo decide solo el servidor** (RF-50, RF-51). Volver de la pasarela no prueba nada: la solicitud pasa a `PAGADA` únicamente cuando el núcleo recibe el evento firmado de la pasarela. Por eso, mientras hay un intento en curso la pantalla dice "Confirmando tu pago" y nunca "pagado".
- **La pasarela se abre en la misma pestaña.** Su `urlRedireccion` devuelve al turista a la solicitud, así que no hace falta otra ventana ni se pierde el contexto. Si el navegador no la abre, queda un enlace para continuar.
- **El total no tiene cargos adicionales para el turista** (RF-49). Es `condiciones.precioBase`; la pantalla aclara que la comisión de la plataforma se descuenta al prestador.
- **La dirección del checkout se revisa antes de abrirla** (`urlDeCheckoutSegura`): debe ser absoluta, `http` o `https` y sin usuario ni contraseña. Un `javascript:` o `data:` no se abre. La pasarela simulada usa `http://localhost`, por eso `http` es válido.

### Cómo funciona

- **Pagar** (P04.1). El bloque aparece con la solicitud en "Por pagar": oferta, fecha, personas y total, cuántos intentos quedan y, antes del último, que si se rechaza la solicitud vencerá. Un doble clic envía una sola petición. Abrir el pago lleva una `Idempotency-Key` y, si ya hay un intento en curso, el núcleo devuelve ese mismo, así que repetir la llamada no cobra dos veces. Si el núcleo dice que ya no se puede pagar (`SOLICITUD_NO_COBRABLE`, `INTENTOS_AGOTADOS`, `PLAZO_VENCIDO`), se avisa y se vuelve a consultar la solicitud.
- **Confirmando tu pago** (RF-50, RF-51). Con un intento `EN_CURSO` se muestra ese aviso y la pantalla reconsulta la solicitud y sus intentos cada 3 segundos durante los primeros 2 minutos y cada 15 después (`esperaDeReconsulta`), hasta que cambien. No consulta con la pestaña oculta y retoma al volver. Mientras tanto se ofrece "Continuar el pago", que reabre el mismo intento. Al quedar `PAGADA` se muestra "Pago confirmado" con el total y la referencia de la transacción (RF-52), y el foco va al aviso para que los lectores de pantalla lo anuncien.
- **Intentos y reintento** (P04.2, RF-58). Se permiten tres intentos con resultado de la pasarela: cuentan `RECHAZADO` y `ERROR`, no `ABANDONADO` (sin resultado a los 30 minutos) ni `EN_CURSO`. La lista muestra cada intento con su estado, su monto, sus fechas y la referencia de la transacción. Con el pago fallido se ofrece "Reintentar pago" mientras queden intentos y plazo; si no, se explica y se ofrece pedir disponibilidad de nuevo. Al tercer rechazo la solicitud queda vencida y el detalle conserva la lista, para que se vea por qué.
- **Mis solicitudes** ofrece "Pagar" a una solicitud por pagar y "Reintentar pago" a una con el pago fallido; los dos llevan al detalle.

La lógica (intentos que quedan, si se puede pagar, motivos, validación del checkout y calendario de reconsulta) está en funciones puras con pruebas propias.

### Para el equipo de backend

- **Motivo del rechazo (opcional).** `IntentoPago` trae solo el estado del intento, no el motivo que dio la pasarela (fondos insuficientes, tarjeta rechazada, etc.). El frontend muestra una explicación fija por estado. Si el equipo quiere mostrar el motivo real, haría falta un campo nuevo en `IntentoPago`.

## Inicio, catálogo y zonas

Pantallas públicas del turista, en `features/turista` (`inicio/`, `catalogo/`, `zona/`).

- **Inicio** (`/`, P01.0). El buscador lleva a `/catalogo?texto=...`; las cuatro tarjetas de tipo, al catálogo con ese tipo (la de paquetes, a `/paquetes`). Solo pide `GET /zonas`: no carga ofertas, no muestra "destacados" ni patrocinados, y **no llama a `POST /recomendaciones`** (cada visita gastaría cupo de la IA y la respuesta sin la consulta del turista sería genérica); en su lugar, la tarjeta "¿No sabes qué elegir?" enlaza a `/recomendaciones`. Si `GET /zonas` falla, la sección de zonas no aparece y el resto sigue.
- **Zonas del Inicio.** Una fila con desplazamiento horizontal, con botones anterior y siguiente y deslizable en el celular. No avanza sola (WCAG 2.2.2).
- **Enlaces viejos.** El catálogo vivía en `/`. Un guard de la ruta `/` (`catalogo-en-la-raiz.ts`) redirige a `/catalogo`, con los mismos parámetros, cualquier raíz que traiga un parámetro del catálogo. La lista de parámetros vive en ese archivo y una prueba la compara con los que escribe el catálogo.
- **Búsqueda por texto** (DC-37). Parámetro `texto`, de 2 a 100 caracteres sin contar los espacios de los extremos. Con un solo carácter se avisa y no se consulta; una URL con un texto fuera de ese rango se ignora.
- **Paquetes** (`/paquetes`, P01.3). Usa el mismo componente del catálogo con el dato de ruta `tipoFijo: 'PAQUETE'`: no ofrece elegir el tipo y la URL no lo repite.
- **Ficha de zona** (`/zonas/:id`, RF-25). `GET /zonas/{zonaId}`. También responde por una zona inactiva (DC-49) y entonces lo avisa. Las "marcas" son `areaProtegida` y `altaAfluencia` del contrato. Las alertas son la misma sección opcional de la ficha de una oferta.
- **Menú.** "Paquetes" está junto a "Catálogo" para quien no ha iniciado sesión, el turista y el prestador. No hay enlace "Inicio": el logo (`Samarian Tour, inicio`) lleva a `/`.

### Para el equipo de backend

- **Orden de los paquetes.** El diseño de referencia traía "Ordenar por" (precio de mayor a menor, más actividades incluidas). `GET /ofertas` solo ordena por precio ascendente y luego por id, así que el selector no se construyó. Haría falta un parámetro `orden` o un conteo de actividades por paquete en `OfertaResumen`.
- **Imagen de la zona (opcional).** `Zona` no trae foto; las tarjetas del Inicio usan seis fondos de color que se repiten. Una portada por zona permitiría mostrarla.

## Recomendaciones y alertas

El turista —con o sin sesión— pide sugerencias, consulta las alertas del destino y ve alternativas a una oferta muy concurrida (RF-26 a RF-29, P08). Está en `features/recomendacion` y todo es público. El servicio es `core/api/recomendacion.service.ts` (`/api/recomendacion/**`); pide siempre los textos en el idioma vigente (`conIdioma` en el cuerpo del `POST`, `?idioma=` en los `GET`). Los tipos salen del contrato (`modelos-recomendacion.ts`). La lógica está en funciones puras con pruebas propias (`consulta.ts`, `conversacion.ts`, `alertas/alertas.ts`, `core/api/degradacion.ts`).

- **Recomendaciones** (`/recomendaciones`, P08.1). Un asistente en forma de conversación: el turista escribe lo que busca (hasta 500 caracteres) o toca una de cuatro ideas, y recibe de 3 a 5 ofertas con la explicación de por qué se eligieron, los criterios que cumplen y quién las eligió (reglas o inteligencia artificial). Los filtros del contrato están plegados en "Afinar la búsqueda": tipo de oferta, tipo de actividad, zona, accesibilidad, esfuerzo máximo y precio máximo; la accesibilidad, el esfuerzo y el precio nunca se relajan. Con menos de 3 coincidencias se muestran las que haya con el aviso de pocas coincidencias, y sin ninguna se lleva al catálogo. Hace falta escribir algo o elegir un filtro. Las tarjetas no muestran calificaciones ni datos del prestador, y no hay ofertas patrocinadas.
- **Alertas del destino** (`/alertas`, P08.2). Las alertas activas agrupadas por zona, las de nivel más grave primero. Cada una dice su nivel (Amarilla, Naranja o Roja, con color, icono y palabra), su tipo, el título y la descripción, **quién la emite** y, si el servicio las da, cuándo se emitió y hasta cuándo vale; arriba, la hora de la última actualización. Una fuente "(simulado)" se muestra tal cual la entrega el servicio: un dato simulado nunca se presenta como oficial (DR-17). Sin alertas dice "Sin alertas activas".
- **Alertas en la ficha** (P08.2). La ficha de una oferta con zona trae la sección "Alertas del destino" de esa zona, con las mismas tarjetas, "Sin alertas activas" y la hora si no hay, y un enlace a `/alertas`.
- **Alternativas** (P08.3). Solo en la ficha de una oferta de una zona de alta afluencia: de 2 a 3 ofertas parecidas en zonas con menos visitantes, con la misma tarjeta y explicación que las recomendaciones. Siempre por regla, sin IA (DR-19). La ficha ni siquiera las pide si la zona no es de alta afluencia.

### Si la recomendación no está disponible (degradación)

El servicio de recomendación es opcional (ADR-04): sin él, el resto de la plataforma debe funcionar. `conDegradacion` le pone un límite de tiempo a cada consulta y la convierte en un `Resultado` (nunca lanza): `ok`, `no-disponible` (por tiempo, servicio o conexión) o `error` propio de la consulta (un dato inválido, una oferta que no existe). Cuentan como **no disponible**: pasar del límite, no tener conexión y un 502, 503 o 504. Al pasar el límite la petición se cancela.

| Dónde | Límite | Si no está disponible |
|---|---|---|
| `POST /recomendaciones` | 10 s | Mientras espera dice "Buscando opciones…". Después, un aviso en la conversación con "Intentar de nuevo" (repite la misma consulta) y un enlace al catálogo. Lo ya conversado se conserva |
| `GET /alertas` (página) | 8 s | Aviso con "Intentar de nuevo" y enlace de vuelta; la página no queda en blanco |
| `GET /zonas/{id}/alertas` (ficha) | 3 s | La sección se oculta, sin avisos ni errores |
| `GET /ofertas/{id}/alternativas` (ficha) | 3 s | La sección se oculta, sin avisos ni errores; también si responde `aplica: false`, 404 o ninguna alternativa |

Las secciones de la ficha no ocupan lugar mientras consultan, así la página no se mueve al llegar, y son independientes entre sí y del resto de la ficha. Hay pruebas del límite con temporizadores simulados (una respuesta que llega un poco antes del límite sí se muestra; al llegar al límite se cancela), de cada código de fallo y de que la ficha sigue completa.

### Decisiones de diseño

- **El menú del turista tiene solo "Recomendaciones"**, como en el documento de diseño (Entregable 2, Tabla 3): Catálogo · Paquetes · Recomendaciones · Mis solicitudes · Opiniones. A `/alertas` se llega con el enlace visible "Ver alertas del destino" del asistente y con el de la sección de la ficha. Quien no ha iniciado sesión, el turista y el prestador ven el enlace; las rutas son públicas.
- **La conversación vive solo en memoria.** Lo que escribe el turista puede incluir datos personales: no se guarda en el navegador ni viaja en la dirección (el servicio usa `POST` por eso, DR-09), y una nota pide no escribirlos. Al recargar, se borra. Una prueba revisa `localStorage` y `sessionStorage`.
- **Los textos del servicio se muestran tal cual.** Las explicaciones, y el título y la descripción de las alertas, los escribe el servicio en el idioma pedido; la fuente no se traduce (DR-21). Lo demás (niveles, tipos, criterios, avisos) sigue el idioma de la interfaz.
- **Una sola tarjeta para recomendaciones y alternativas** (`tarjeta-recomendada`), porque el contrato les da la misma forma; y una sola para las alertas (`tarjeta-alerta`), compartida por la página y la ficha.
- **Lo que el diseño de referencia traía y no se construyó:** `alertas.html` no existe en `_referencia-html/`, así que la página usa el estilo del resto de la interfaz.

### Supuestos del frontend

- **[SUPUESTO] El 504 también es "no disponible"**: es el mismo caso que un 502 o un 503 (el Nginx se cansó de esperar al servicio).
- **[SUPUESTO] Las zonas con alertas se ordenan por gravedad**, de la más grave a la menos grave; a igual nivel se conserva el orden del servicio (el de su id).
- **[SUPUESTO] El límite de 10 s del `POST /recomendaciones`** se eligió porque el servicio puede tardar hasta unos 9 s (catálogo 3 s, IA 6 s y luego reglas; ver `docs/decisiones-recomendacion.md`).
- Una zona sin nombre en el resumen de alertas se muestra como "Zona sin nombre".

### Para el equipo de backend

- **Margen del `POST /recomendaciones`.** En el peor caso el servicio tarda unos 9 s y el frontend espera 10 s: queda 1 s de margen, y el Nginx (`proxy_read_timeout`) o cualquier retraso de red lo puede consumir. Conviene confirmar que la ruta `/api/recomendacion/` tiene un tiempo máximo mayor y, si se puede, acortar el camino de la IA.
- **Nombre de la zona en las alertas.** `ZonaConAlertas.zonaNombre` se omite si el catálogo no lo pudo dar; el turista solo vería un id. El frontend lo muestra como "Zona sin nombre". Conviene que el servicio siempre lo entregue o lo pida al catálogo con un respaldo.
- **504.** El contrato solo documenta el 503 (`RECOMENDACION_NO_DISPONIBLE`, `ALERTAS_NO_DISPONIBLES`). Si el Nginx devuelve 502 o 504 cuando el servicio está caído, conviene documentarlo.
- **Fecha de la alerta del IDEAM.** Las alertas del IDEAM no traen fecha de emisión ni de fin (DR-15); la ficha solo puede mostrar la hora de consulta de la fuente, no desde cuándo está vigente.

## Diseño

Los colores, la tipografía y las piezas comunes (tarjetas, etiquetas, botones, campos, alertas) están en `src/styles.css`, como variables y clases globales; cada pantalla solo agrega su propia disposición. Los iconos son SVG en línea (`shared/icono`), sin imágenes externas. Las ofertas con fotos muestran su portada; una versión sin fotos (anterior a DC-47) se dibuja con el icono de su tipo.

- **Tipografía.** Manrope en los títulos (`h1` a `h3`, pesos 700 y 800) e Inter en el texto. Las dos se cargan desde `@fontsource` en la lista `styles` de `angular.json`, y las variables `--fuente-titulos` y `--fuente-texto` las nombran.
- **Variables en `:root`.** Colores de marca (`--acento`, `--enlace`, `--profundo`), estados (`--peligro`, `--aviso-*`, `--exito-*`, `--info-*`), radios (`--radio-sm` a `--radio-pildora`) y sombras (`--sombra-1` a `--sombra-3`). `--enlace` (#0b7373) es el turquesa que lleva texto blanco; el turquesa de marca (`--acento`) va en fondos grandes e iconos.
- **Contraste.** El texto blanco va solo sobre `--enlace` o más oscuro. Los campos conservan el borde `--borde-control` para que se distingan del fondo (contraste 3:1), y el contorno de `:focus-visible` se mantiene en todos los controles.
- **Cabecera y pie.** Están en `src/app/app.html` y `app.css`. La cabecera no es fija al hacer scroll para no tapar la tarjeta lateral de la ficha.

## Fotos de las ofertas

Una oferta lleva de 1 a 6 fotos y la primera es la portada (DC-47).

- **Subir.** En el formulario de publicar y editar, el selector de fotos (`features/prestador/formulario-oferta/selector-fotos`) sube cada foto apenas se elige, con `POST /prestador/fotos`, una por una para respetar el orden en que se eligieron. La API devuelve su `id` y las direcciones de sus dos tamaños. La foto queda suelta y la API la borra si en 24 horas ninguna oferta la usa.
- **Orden.** Se cambia con los botones "Mover antes", "Mover después" y "Quitar", no arrastrando, para que funcione con teclado y en celular. La foto que queda primera es la portada.
- **Enviar.** Al publicar o editar solo viajan los `id` de las fotos, en orden. El formulario no envía mientras haya una subida en curso y marca el campo si no hay ninguna foto. Cambiar las fotos de una oferta es una edición y pasa por moderación.
- **Reglas en el navegador.** JPG, PNG o WebP de hasta 5 MB y máximo 6 fotos (`fotos-oferta.ts`). Es solo una ayuda para avisar antes de enviar: la API vuelve a validar y reconoce el formato por el contenido. Si un archivo pasa de 6 MB, Nginx responde el mismo error `DATO_INVALIDO` con el campo `foto`.
- **Mostrar.** Las tarjetas del catálogo usan la miniatura de `portada` (hasta 400 px) y, sin portada, el icono del tipo. La ficha usa `shared/galeria-fotos`: un mosaico de hasta 3 fotos y un visor a pantalla completa, con el elemento `<dialog>`, que se navega con botones o con las flechas del teclado.
- **Perfil público.** El mismo selector se usa en el perfil del prestador (P03.2), con otras rutas (`POST /prestador/perfil/fotos` y `DELETE …/fotos/{id}`): cada foto se sube y se quita al instante, sin orden ni portada, y el perfil admite de 0 a 6 fotos.

## Sesión y cuentas

El núcleo emite el token al iniciar sesión (`POST /api/nucleo/cuentas/sesion`) y las rutas `/prestador/**`, `/turista/**`, `/operador/**`, `/gestor/**` y `/admin/**` de los tres servicios lo exigen en `Authorization: Bearer` (DN-37). Ya no existe el selector "Usuario demo" ni los headers `X-Usuario-Id`, `X-Rol` y `X-Tipo-Prestador`. El catálogo público, la ficha y las recomendaciones siguen abiertos sin sesión.

- **Servicio de sesión** (`core/sesion/sesion.service.ts`). Guarda la cuenta, el rol y el tipo de prestador como signals. El tipo de prestador se lee del token (solo lo trae el prestador que ya registró su RNT); no valida la firma, eso lo hace el servidor.
- **Dónde se guarda.** Con "Recordarme" desmarcado, en `sessionStorage`: sobrevive a recargar la página pero no a cerrar la pestaña. Marcado, en `localStorage` hasta que venza la renovación (7 días). Si el navegador bloquea el almacenamiento, la sesión dura lo que dure la página. Las pestañas se avisan entre sí al iniciar, renovar o cerrar la sesión.
- **Renovación.** El token de acceso vale 15 minutos y se renueva solo un minuto antes de vencer (`POST /cuentas/sesion/renovacion`) y al arrancar la aplicación si ya venció. El token de renovación sirve una sola vez y usar uno gastado cierra la sesión (DN-29), así que solo hay **una renovación a la vez** y varias peticiones comparten su resultado.
- **Interceptor** (`core/sesion/sesion.interceptor.ts`). Manda el token a `/api/**`, menos a las rutas de acceso (`/cuentas/*` y `/politica-datos/*`). Ante un 401 `NO_AUTENTICADO` renueva y repite la petición **una sola vez**. Si la renovación es rechazada, en una ruta protegida lleva a `/iniciar-sesion?volver=…` y en una pública (el catálogo) repite sin token. Otros 401, como `CONTRASENA_INCORRECTA`, no renuevan.
- **Guards** (`core/sesion/guards.ts`). Cada sección es de un rol: sin sesión llevan a iniciar sesión (recordando la página) y con otro rol, a `/sin-permiso`. El servidor vuelve a comprobar el rol en cada petición. Gestor y administrador tienen una pantalla "próximamente" que reemplazarán sus ramas.
- **Regreso seguro.** `?volver=` solo acepta rutas internas de la aplicación (`rutaSegura`): rechaza `https://…`, `//…` y `javascript:`, las pantallas de acceso y las secciones de otro rol.
- **Cabecera.** Sin sesión: "Iniciar sesión" y "Registrarse". Con sesión: los enlaces del rol (`core/sesion/enlaces-por-rol.ts`) y un menú con el nombre, "Mis datos" y "Cerrar sesión".

Las cuentas de la demo (clave `Demo1234`, todas `@demo.samariantour.co`): prestadores `agencia`, `guia`, `hotel`, `restaurante`, `agencia-sin-rnt`; operadores `operador1` y `operador2`; `gestor`; turistas `laura`, `andres`, `laura.gomez`, `carlos.perez`, `ana.torres`. El administrador es `admin@samariantour.co` con `SamarianAdmin2026`. Los correos de recuperación llegan a Mailpit (http://localhost:8025) y el código de WhatsApp sale en `docker compose -f infra/docker-compose.yml logs nucleo`.

### Pantallas de cuenta

- **Registro** (RF-01 a RF-03, RF-09 a RF-11). Turista o prestador, con la versión vigente de la política de datos (`GET /politica-datos/vigente`), que se lee en un `<dialog>`. El turista da su WhatsApp con indicativo (`+573001234567`); al prestador no se le pide. Al terminar, la persona entra sola con los datos que escribió: el turista va a verificar su WhatsApp y el prestador a su panel.
- **Iniciar sesión** (RF-04, RF-05). Con cinco intentos fallidos el núcleo bloquea la cuenta 15 minutos; la pantalla dice cuánto falta.
- **Recuperar la contraseña** (RF-07, RF-08). `/recuperar-contrasena` pide el correo y confirma sin revelar si existe. `/restablecer-contrasena?token=…` pone la contraseña nueva: el enlace vale 30 minutos y sirve una vez. Al terminar se cierran todas las sesiones de la cuenta.
- **Mis datos** (RF-12). Consultar y corregir el nombre y, el turista, su WhatsApp; un número nuevo queda sin verificar y se confirma con el código de 6 dígitos. El correo no se cambia (DN-31). De los seis derechos del titular (Ley 1581), Acceso, Rectificación y Supresión tienen acción; Oposición, Portabilidad y Revocar la autorización se explican sin botón porque el contrato no tiene endpoint.
- **Eliminar la cuenta** (RF-13). El turista y el prestador confirman con su contraseña (`POST /turista/supresion` o `/prestador/supresion`). Si el prestador tiene compromisos pendientes, el núcleo los lista y no deja eliminar. Operador, gestor y administrador no tienen esta opción.

## Idiomas

La interfaz está en español e inglés (RF-18, RF-19) y cambia al instante, sin recargar. No usa librerías: es un mecanismo propio y liviano en `src/app/core/idioma`.

- **Idioma vigente** (`idioma.ts`). Un signal de módulo, para que también lo lean las tablas de etiquetas, `ErrorApi` y las funciones puras que arman mensajes, que no tienen inyección de dependencias. `IdiomaService` es la fachada para los componentes: `idioma()`, `establecer()` y `locale()` (`es-CO` o `en-US`, para `Intl` y los pipes de fecha y precio).
- **Cuál se usa.** La elección guardada en `localStorage` (`samarian-tour.idioma`); si no hay, el idioma del navegador cuando es `es` o `en`; si no, español. Al cambiar, el servicio también cambia el atributo `lang` de la página y su descripción.
- **Diccionarios** (`*.i18n.ts`). Uno por área, con las claves `area.pantalla.texto`. El español fija las claves y el inglés debe tener exactamente las mismas: si falta una traducción o sobra una clave, **el build falla**. Se reúnen en `textos.ts`. Los textos admiten parámetros (`{n}`) y plural (`{ uno, otros }`).
- **En las plantillas,** el pipe `t`: `{{ 'comun.cerrar' | t }}`, con parámetros `{{ 'comun.pagina' | t: { pagina: 2, total: 5 } }}`. Solo acepta claves que existan. Es impuro a propósito, porque un pipe puro no se volvería a evaluar al cambiar el idioma. **En código,** `traducir('comun.cerrar')`.
- **Glosario.** Los términos del inglés siguen `core/idioma/GLOSARIO.md`, y `textos.spec.ts` lo revisa: parámetros iguales en los dos idiomas, ningún texto sin traducir y términos del glosario.
- **Selector.** `ES | EN` en la cabecera (`shared/selector-idioma`): dos botones con `aria-pressed`, cada uno con su `lang`. El cambio es inmediato y se recuerda.
- **Etiquetas de enumeraciones** (`shared/etiquetas.ts`). Son tablas que se leen en el idioma vigente cada vez que se consultan (`tablaDeTextos`), así que se usan como siempre (`ROL[rol]`) y siguen el idioma sin tocar las plantillas. Cada tabla exige todos los valores del contrato.
- **Títulos de las pestañas.** El `title` de cada ruta es una clave (`app.titulo.catalogo`), no un texto; `EstrategiaDeTitulo` lo traduce y lo mantiene al cambiar de idioma. `app.titulos.spec.ts` revisa que toda ruta tenga una clave que exista.
- **Errores de la API.** `ErrorApi` guarda la clave del mensaje y arma `mensaje` al leerlo (`core/idioma/errores.i18n.ts`, un mensaje por código del contrato: si falta uno, el build falla). Las pantallas guardan en sus señales un `TextoVivo` (una función, `() => error.mensaje`) o el propio `ErrorApi`, y la plantilla lo llama: así un mensaje que ya está en pantalla cambia de idioma sin repetir la acción.
- **Fechas, precios y números.** El `LOCALE_ID` de Angular no puede cambiar en marcha, así que se usan los pipes propios `fecha`, `precio`, `numero` y `unidad`: `$ 150.000` y `5 nov 2026` en español; `COP 150,000` y `Nov 5, 2026` en inglés. No se usa el pipe `date` de Angular.
- **A la API.** El interceptor `idiomaInterceptor` manda `Accept-Language` en toda petición a `/api/`. La recomendación además recibe el idioma en el cuerpo (`conIdioma`) y como parámetro `?idioma=` en alertas y alternativas (`parametrosConIdioma`), en `core/idioma/idioma-api.ts`, para la rama de recomendación.
- **Pruebas.** `src/test-setup.ts` deja el idioma en español antes de cada prueba; una prueba que necesite el inglés usa `establecerIdioma('en', { recordar: false })`.
- **Lo que no se traduce.** Lo que viene del backend se muestra tal como llega: nombres y descripciones de ofertas, zonas, buenas prácticas, perfiles, el texto de la política de datos, los compromisos del prestador, el motivo de un dato inválido y el motivo de un rechazo. Sí se traducen los códigos de idioma de atención (`es` → Español / Spanish) y las unidades de precio conocidas (`por persona` → per person).

Regla para las ramas nuevas: **todo texto de la interfaz va en los dos idiomas, en el diccionario de su área y con el pipe `t`; ningún texto suelto en las plantillas ni en el código.**

## Tamaño de la aplicación

El presupuesto de `angular.json` avisa cuando el paquete inicial pasa de **600 kB** y falla a partir de 1 MB (antes el aviso era a 500 kB; el paquete inicial está hoy en unos 517 kB). La causa es que los diccionarios de textos de todas las áreas (`core/idioma/*.i18n.ts`, en español e inglés) se cargan juntos en el paquete inicial, aunque las pantallas de cada rol sean rutas perezosas.

**Mejora pendiente:** cargar los diccionarios de cada área junto con sus rutas perezosas (por ejemplo, el de `admin` solo al abrir `/admin`), en lugar de registrarlos todos en `textos.ts`. Hasta entonces el tamaño crece con cada pantalla nueva.

## Errores de la API

El interceptor de errores lee el `application/problem+json` y traduce su `codigo` a un mensaje (`core/errores/error-api.ts`). El código nunca se muestra. Con `DATO_INVALIDO`, la API nombra el campo en `detail` (`campo: detalle`) y el formulario marca ese campo. Tres códigos del núcleo traen datos extra que `ErrorApi` conserva en `extras`: `bloqueadaHasta` (`CUENTA_BLOQUEADA`), `intentosRestantes` (`CODIGO_INCORRECTO`) y `compromisos` (`PRESTADOR_CON_COMPROMISOS`). Cada código tiene su mensaje en español y en inglés (ver "Idiomas").

## Supuestos

Lo que el contrato no cubre se resolvió en el frontend y está marcado con `[SUPUESTO]` en el código:

- **Texto alternativo de las fotos.** La API no guarda una descripción de cada foto, así que el texto alternativo es genérico: la posición de la foto, el total y el nombre de la oferta. En las tarjetas del catálogo la imagen es decorativa.
- **Unidad del precio en "Mis ofertas".** La lista de ofertas propias no trae `unidadPrecio`; se toma de la tabla por tipo de oferta de DC-22.
- **Estado de la oferta.** Se calcula con `retirada`, `publicada` y `pendiente`. Si cumple más de una definición manda este orden: retirada, rechazada, publicada con cambios en revisión, en moderación, publicada.
- **Tipo de prestador.** La cuenta no lo trae; se lee del token. Un prestador recién registrado no lo tiene hasta registrar su RNT en la sección RNT, y mientras tanto el panel y el formulario le explican que no puede publicar. Como el tipo viaja en el token, tras registrar el RNT o cambiar el tipo se renueva la sesión.
- **Ofertas sin RNT activo.** El dashboard dice que con el RNT inactivo, no encontrado o sin registrar las ofertas no se muestran en el catálogo (RF-77); lo calcula con `estadoRnt`, no con `habilitado`.
- **Idiomas del guía y redes del perfil.** Los idiomas del guía se escriben como códigos de dos letras separados por comas (`es, en`), igual que en las ofertas; las redes sociales del perfil, una dirección por línea.
- **Bancos de la cuenta de cobro.** El contrato recibe la entidad como texto libre (hasta 100 caracteres). Se sugieren los bancos más usados y el prestador puede escribir otro.
- **WhatsApp y cuenta de cobro sin RNT.** El contrato documenta `PRESTADOR_NO_REGISTRADO` en los endpoints de WhatsApp y al guardar la cuenta de cobro; al leerla, el frontend también lo trata así. En los tres casos las pantallas lo explican y enlazan a la sección RNT.
- **Un solo nombre en el registro.** El núcleo guarda un único `nombre`: el turista escribe su nombre completo y el prestador, el nombre comercial de su negocio. El diseño separa nombres y apellidos.
- **Contraseña.** De 8 a 72 caracteres con al menos una letra y un número (DN-28); el núcleo las vuelve a aplicar. El WhatsApp se acepta con espacios o guiones y se envía en formato internacional.
- **Texto de la política.** Llega en Markdown y se muestra tal cual, respetando los saltos de línea, sin interpretarlo.
- **Unidad del precio y códigos de idioma.** La API entrega `unidadPrecio` en español y `idiomasAtencion` como códigos (`es`, `en`). El frontend traduce las cuatro unidades conocidas y los dos códigos, y deja tal cual lo demás.
- **Precio de referencia en el formulario.** La oferta trae un precio con su unidad (DC-22). El total de lo pedido es informativo: precio por personas en actividad, paquete y restaurante, y precio por noches y unidades en alojamiento. El resumen lo llama "Precio de referencia" y dice que el operador confirma el precio final.
- **Total a pagar.** Se toma `condiciones.precioBase` como lo que se cobra, tal como dice el contrato (RF-46); no se calcula ningún desglose.
- **Filtros de mis solicitudes.** El contrato no filtra por estado: Activas y Finalizadas se filtran en el navegador, solo sobre la página cargada.
- **Nombre de la zona.** La solicitud trae solo `zonaId`; el nombre se busca en el catálogo.
- **Referencia de la solicitud.** Se muestran las primeras 8 letras de su identificador para citarla.
- **Cuentas internas.** Operador, gestor y administrador no tienen ruta de supresión: su cuenta la gestiona un administrador.
- **Orden de la cola.** La API entrega la cola de la más antigua a la más reciente y el frontend la reordena por urgencia solo dentro de la página cargada, así que una urgente de la página 2 no sube a la página 1. En la práctica el orden coincide, porque el plazo del operador es siempre de 24 horas desde que se crea la solicitud.
- **Datos de un aviso.** Los avisos del operador leen las claves `nombreOferta`, `fechaInicio`, `horaSalida` o `horaReserva` y `personas` de `datos`; el contrato no las fija.
- **Cálculo de referencia al atender.** El precio del catálogo por lo pedido (personas, o noches por unidades en alojamiento) se muestra al operador como orientación; el precio total que se envía lo escribe él.
- **Motivo de un intento de pago.** El contrato no entrega el texto de la pasarela, solo el estado del intento (`RECHAZADO`, `ERROR`, `ABANDONADO`…); el motivo que se muestra es una explicación fija de cada estado.
- **Web Checkout, no widget.** El núcleo entrega también `llavePublica` y `firmaIntegridad` para el widget de Wompi; el frontend no los usa y abre siempre `checkout.url`, que sirve igual para la pasarela simulada y para Wompi.
- **Intentos del pago.** Se piden con la solicitud confirmada, con el pago fallido, pagada o vencida (para mostrar la referencia de la transacción o por qué venció) y en ningún otro estado. Si no se pueden leer, se conservan los últimos que se leyeron.
- **Zonas incompletas.** Si una zona del catálogo no tiene descripción o ubicación, no se puede inactivar ni reactivar desde la lista (el reemplazo completo exigiría inventar datos): la tarjeta pide completarla editándola.
- **Estado de las cuentas internas y de las colas de moderación.** Se muestran tal como las entrega la API; los cambios de moderación se ven cuando el catálogo aplica la decisión.
- **Plazo con minutos.** Los plazos se escriben con las dos unidades mayores ("20 h 0 min"), como en las solicitudes del turista.
- **Periodo de los indicadores.** El contrato no dice si la fecha final puede ser futura; el frontend no deja enviarla. Una dirección con solo una de las dos fechas, o con un periodo inválido, usa los últimos 30 días.
- **Nombre de las zonas del tablero.** Se toma de la lista pública de zonas (solo activas); sin él se muestra el `zonaId`.
- **Conversiones calculadas.** El paso de una etapa del embudo a la siguiente y el porcentaje de pago por categoría no los entrega el núcleo: se calculan en el frontend, solo con cantidades conocidas, y con un decimal.
- **Orden de las categorías.** De la más grande a la más pequeña, no el del núcleo.

## Docker

`Dockerfile` compila la aplicación con Node y la copia a `nginx:1.27-alpine`. La configuración de Nginx no va en la imagen: Docker Compose monta `infra/nginx/nginx.conf`.
