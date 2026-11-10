# Frontend de Samarian Tour

Aplicación Angular contra la API real: el turista busca ofertas, ve su ficha con galería de fotos, mapa y opiniones, y opina sobre las que pagó; el prestador publica con sus fotos, ve sus ofertas, las edita y las retira. Cada persona tiene su cuenta: se registra, inicia sesión, recupera su contraseña, consulta y corrige sus datos y puede eliminar su cuenta.

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
│   ├── api/                 tipos generados, servicios HTTP del catálogo, del prestador, de opiniones, de cuentas, de solicitudes y del operador
│   ├── tiempo/              reloj que se renueva solo, para los plazos
│   ├── errores/             interceptor que traduce el problem+json a un mensaje para el usuario
│   ├── sesion/              sesión con token, interceptor, guards por rol, rutas y enlaces por rol
│   └── avisos/              mensajes breves de confirmación
├── shared/                  piezas reutilizables: mapa, galería de fotos, menú de la cuenta, marco de las pantallas de acceso, "próximamente", iconos, estrellas, paginación, diálogo de confirmación, pasos de la solicitud, pipes y textos
└── features/
    ├── turista/             catálogo (/), ficha con opiniones (/ofertas/:id), mis opiniones (/mis-opiniones) y las solicitudes de disponibilidad (solicitudes/)
    ├── prestador/           panel, mis ofertas, detalle y formulario de publicar o editar
    ├── operador/            panel con menú lateral, cola, historial, atender una solicitud y avisos
    └── cuentas/             iniciar sesión, registro, recuperar la contraseña, mis datos y "no tienes permiso"
```

| Ruta | Quién | Pantalla |
|---|---|---|
| `/` | Todos | Catálogo con filtros en los query params y paginación de 12; cada tarjeta muestra la miniatura de la portada |
| `/ofertas/:id` | Todos | Ficha con galería de fotos, zona, perfil del prestador, mapa y opiniones; el turista con sesión puede "Dejar una opinión" |
| `/iniciar-sesion` | Todos | P06.2 Iniciar sesión (`?volver=` lleva a la página que se quería ver) |
| `/registro` | Todos | P06.1 Registro de turista o prestador (`?tipo=prestador` deja elegido el prestador) |
| `/recuperar-contrasena` | Todos | P06.3 Pedir el enlace para elegir una contraseña nueva |
| `/restablecer-contrasena?token=…` | Todos | Poner la contraseña nueva; es el enlace que envía el correo del núcleo |
| `/mis-datos` | Con sesión | P06.4 Mis datos y derechos, verificar el WhatsApp y eliminar la cuenta |
| `/sin-permiso` | Con sesión | "No tienes permiso" cuando se abre la sección de otro rol |
| `/mis-opiniones` | Turista | Opiniones del turista, con su estado y el motivo si fue rechazada |
| `/turista/solicitar/:ofertaId` | Turista | P02.1 Formulario de solicitud de disponibilidad (el botón de la ficha lleva aquí) |
| `/turista/solicitudes` | Turista | P02.2 Mis solicitudes: los 9 estados, filtros Todas / Activas / Finalizadas y plazos |
| `/turista/solicitudes/:id` | Turista | P02.2 Detalle: estado, historial, plazo y condiciones; P02.3 confirmar o declinar si está atendida. El núcleo enlaza aquí desde el pago y el WhatsApp |
| `/prestador/ofertas` | Prestador | Mis ofertas, con su estado y las acciones |
| `/prestador/ofertas/nueva` | Prestador | Publicar una oferta, con sus fotos |
| `/prestador/ofertas/:id` | Prestador | Detalle: versión publicada y versión pendiente |
| `/prestador/ofertas/:id/editar` | Prestador | Editar una oferta |
| `/operador` | Operador | P02.4 Cola: las solicitudes pendientes, las urgentes (6 h o menos) primero y resaltadas, con paginación |
| `/operador/historial?estado=` | Operador | Historial: la misma consulta de la cola con un estado, de la más reciente a la más antigua |
| `/operador/solicitudes/:id` | Operador | P02.5 Atender: contactar al prestador por WhatsApp y registrar "hay cupo" (con precio total y condiciones) o "no hay cupo" |
| `/operador/avisos` | Operador | Avisos de solicitudes nuevas, del más reciente al más antiguo |
| `/gestor/**` | Gestor | Panel de indicadores (P09.1): por ahora "próximamente" |
| `/admin/**` | Administrador | Administración (P05.1): por ahora "próximamente" |

## Solicitudes de disponibilidad

El turista pide disponibilidad sobre una oferta (RF-30 a RF-34), el operador la atiende y el turista confirma o declina las condiciones (RF-43). Lo que cubre esta parte del flujo, en `features/turista/solicitudes`:

- **Formulario** (`/turista/solicitar/:ofertaId`). Pide solo lo que el contrato necesita y cambia con el tipo de oferta: actividad y paquete, la hora de salida (una de las de la oferta); alojamiento, la fecha de salida y las unidades; restaurante, la hora de la reserva. El nombre y el contacto salen de la cuenta. Con una sola hora de salida y las personas en 1 solo hay que elegir la fecha (QAS-08). Sin sesión, el botón de la ficha lleva a iniciar sesión y vuelve; sin el WhatsApp verificado no se envía.
- **Mis solicitudes y detalle.** Cada uno de los 9 estados tiene su etiqueta, color y explicación (`estado-solicitud.ts`). Los plazos se cuentan en vivo (`plazo.ts`, `core/tiempo/reloj.ts`). Se vuelve a consultar el estado real al regresar a la pestaña, al reconectarse y cuando un plazo termina (`core/api/reconsulta.ts`, RF-33).
- **Confirmar o declinar** (`responder-condiciones`). Solo en una solicitud atendida y dentro del plazo. Declinar pide confirmación. Si el núcleo dice que algo cambió (plazo vencido, estado distinto), se explica y se consulta de nuevo.
- **Idempotencia.** Enviar y responder llevan una `Idempotency-Key` (ADR-04). `ClaveDeIdempotencia` repite la clave mientras el cuerpo no cambie, y la renueva si el usuario corrige algo, para no recibir un 422.
- **Fechas.** La primera fecha posible es dos días calendario después de hoy **en la hora de Colombia**, no la del navegador.
- **El pago** no está en esta rama: una solicitud confirmada o con el pago fallido solo ofrece ver la oferta hasta que llegue la rama de pagos.

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

## Errores de la API

El interceptor de errores lee el `application/problem+json` y traduce su `codigo` a un mensaje (`core/errores/error-api.ts`). El código nunca se muestra. Con `DATO_INVALIDO`, la API nombra el campo en `detail` (`campo: detalle`) y el formulario marca ese campo. Tres códigos del núcleo traen datos extra que `ErrorApi` conserva en `extras`: `bloqueadaHasta` (`CUENTA_BLOQUEADA`), `intentosRestantes` (`CODIGO_INCORRECTO`) y `compromisos` (`PRESTADOR_CON_COMPROMISOS`). Cada código tiene su mensaje en español y en inglés (ver "Idiomas").

## Supuestos

Lo que el contrato no cubre se resolvió en el frontend y está marcado con `[SUPUESTO]` en el código:

- **Detalle de una oferta propia.** No hay un `GET` de una sola oferta del prestador: se pide la lista y se busca por `id`.
- **Edición.** La API no devuelve el contenido completo de una oferta propia. El formulario se llena con la ficha pública (`GET /ofertas/{id}`), es decir, con la versión publicada. Por eso una oferta que solo tiene versión pendiente no ofrece "Editar", y tampoco se puede editar una oferta publicada que en ese momento no es visible en el catálogo.
- **Fotos al editar.** Las fotos que el formulario muestra al editar salen de la ficha pública, igual que el resto de los datos. Una versión publicada sin fotos (anterior a DC-47) se abre sin fotos y el prestador debe agregar al menos una para guardar.
- **Texto alternativo de las fotos.** La API no guarda una descripción de cada foto, así que el texto alternativo es genérico: la posición de la foto, el total y el nombre de la oferta. En las tarjetas del catálogo la imagen es decorativa.
- **Unidad del precio en "Mis ofertas".** La lista de ofertas propias no trae `unidadPrecio`; se toma de la tabla por tipo de oferta de DC-22.
- **Estado de la oferta.** Se calcula con `retirada`, `publicada` y `pendiente`. Si cumple más de una definición manda este orden: retirada, rechazada, publicada con cambios en revisión, en moderación, publicada.
- **Tipo de prestador.** La cuenta no lo trae; se lee del token. Un prestador recién registrado no lo tiene hasta registrar su RNT (P03.1, otra rama), y mientras tanto el panel y el formulario le explican que no puede publicar.
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
- **Plazo con minutos.** Los plazos se escriben con las dos unidades mayores ("20 h 0 min"), como en las solicitudes del turista.

## Docker

`Dockerfile` compila la aplicación con Node y la copia a `nginx:1.27-alpine`. La configuración de Nginx no va en la imagen: Docker Compose monta `infra/nginx/nginx.conf`.
