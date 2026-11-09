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
│   ├── api/                 tipos generados, servicios HTTP del catálogo, del prestador, de opiniones y de cuentas
│   ├── errores/             interceptor que traduce el problem+json a un mensaje para el usuario
│   ├── sesion/              sesión con token, interceptor, guards por rol, rutas y enlaces por rol
│   └── avisos/              mensajes breves de confirmación
├── shared/                  piezas reutilizables: mapa, galería de fotos, menú de la cuenta, marco de las pantallas de acceso, "próximamente", iconos, estrellas, paginación, diálogo de confirmación, pipes y textos
└── features/
    ├── turista/             catálogo (/), ficha con opiniones (/ofertas/:id) y mis opiniones (/mis-opiniones)
    ├── prestador/           panel, mis ofertas, detalle y formulario de publicar o editar
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
| `/turista/**` | Turista | Mis solicitudes (P02.2): por ahora "próximamente". El núcleo enlaza a `/turista/solicitudes/:id` |
| `/prestador/ofertas` | Prestador | Mis ofertas, con su estado y las acciones |
| `/prestador/ofertas/nueva` | Prestador | Publicar una oferta, con sus fotos |
| `/prestador/ofertas/:id` | Prestador | Detalle: versión publicada y versión pendiente |
| `/prestador/ofertas/:id/editar` | Prestador | Editar una oferta |
| `/operador/**` | Operador | Cola de solicitudes (P02.4): por ahora "próximamente" |
| `/gestor/**` | Gestor | Panel de indicadores (P09.1): por ahora "próximamente" |
| `/admin/**` | Administrador | Administración (P05.1): por ahora "próximamente" |

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
- **Guards** (`core/sesion/guards.ts`). Cada sección es de un rol: sin sesión llevan a iniciar sesión (recordando la página) y con otro rol, a `/sin-permiso`. El servidor vuelve a comprobar el rol en cada petición. Operador, gestor y administrador tienen una pantalla "próximamente" que reemplazarán sus ramas.
- **Regreso seguro.** `?volver=` solo acepta rutas internas de la aplicación (`rutaSegura`): rechaza `https://…`, `//…` y `javascript:`, las pantallas de acceso y las secciones de otro rol.
- **Cabecera.** Sin sesión: "Iniciar sesión" y "Registrarse". Con sesión: los enlaces del rol (`core/sesion/enlaces-por-rol.ts`) y un menú con el nombre, "Mis datos" y "Cerrar sesión".

Las cuentas de la demo (clave `Demo1234`, todas `@demo.samariantour.co`): prestadores `agencia`, `guia`, `hotel`, `restaurante`, `agencia-sin-rnt`; operadores `operador1` y `operador2`; `gestor`; turistas `laura`, `andres`, `laura.gomez`, `carlos.perez`, `ana.torres`. El administrador es `admin@samariantour.co` con `SamarianAdmin2026`. Los correos de recuperación llegan a Mailpit (http://localhost:8025) y el código de WhatsApp sale en `docker compose -f infra/docker-compose.yml logs nucleo`.

### Pantallas de cuenta

- **Registro** (RF-01 a RF-03, RF-09 a RF-11). Turista o prestador, con la versión vigente de la política de datos (`GET /politica-datos/vigente`), que se lee en un `<dialog>`. El turista da su WhatsApp con indicativo (`+573001234567`); al prestador no se le pide. Al terminar, la persona entra sola con los datos que escribió: el turista va a verificar su WhatsApp y el prestador a su panel.
- **Iniciar sesión** (RF-04, RF-05). Con cinco intentos fallidos el núcleo bloquea la cuenta 15 minutos; la pantalla dice cuánto falta.
- **Recuperar la contraseña** (RF-07, RF-08). `/recuperar-contrasena` pide el correo y confirma sin revelar si existe. `/restablecer-contrasena?token=…` pone la contraseña nueva: el enlace vale 30 minutos y sirve una vez. Al terminar se cierran todas las sesiones de la cuenta.
- **Mis datos** (RF-12). Consultar y corregir el nombre y, el turista, su WhatsApp; un número nuevo queda sin verificar y se confirma con el código de 6 dígitos. El correo no se cambia (DN-31). De los seis derechos del titular (Ley 1581), Acceso, Rectificación y Supresión tienen acción; Oposición, Portabilidad y Revocar la autorización se explican sin botón porque el contrato no tiene endpoint.
- **Eliminar la cuenta** (RF-13). El turista y el prestador confirman con su contraseña (`POST /turista/supresion` o `/prestador/supresion`). Si el prestador tiene compromisos pendientes, el núcleo los lista y no deja eliminar. Operador, gestor y administrador no tienen esta opción.

## Errores de la API

El interceptor de errores lee el `application/problem+json` y traduce su `codigo` a un mensaje (`core/errores/error-api.ts`). El código nunca se muestra. Con `DATO_INVALIDO`, la API nombra el campo en `detail` (`campo: detalle`) y el formulario marca ese campo. Tres códigos del núcleo traen datos extra que `ErrorApi` conserva en `extras`: `bloqueadaHasta` (`CUENTA_BLOQUEADA`), `intentosRestantes` (`CODIGO_INCORRECTO`) y `compromisos` (`PRESTADOR_CON_COMPROMISOS`). Los mensajes están en español; la rama de idiomas los traducirá.

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
- **Cuentas internas.** Operador, gestor y administrador no tienen ruta de supresión: su cuenta la gestiona un administrador.

## Docker

`Dockerfile` compila la aplicación con Node y la copia a `nginx:1.27-alpine`. La configuración de Nginx no va en la imagen: Docker Compose monta `infra/nginx/nginx.conf`.
