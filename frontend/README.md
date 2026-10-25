# Frontend de Samarian Tour

Aplicación Angular con las funciones del catálogo contra la API real: el turista busca ofertas, ve su ficha con mapa y sus opiniones, y opina sobre las que pagó; el prestador publica, ve sus ofertas, las edita y las retira.

Angular 22 con componentes standalone, signals, formularios reactivos y carga diferida por funcionalidad. El mapa usa Leaflet con OpenStreetMap, sin API key.

## Requisitos

Node.js 24 (o 22.22 en adelante) y npm. El backend debe estar levantado en `http://localhost:8080` con el perfil `demo` (ver el [README principal](../README.md#levantar-el-piloto)).

## Comandos

```bash
npm ci             # instalar dependencias
npm start          # desarrollo en http://localhost:4200, con recarga al guardar
npm run build      # compilación de producción en dist/samarian-tour/browser
npm test           # pruebas unitarias con Vitest (en CI: npm test -- --no-watch)
npm run gen:api    # regenerar los tipos de la API desde el contrato
```

## Conexión con el backend

- La base de la API es `/api/catalogo`, siempre relativa al origen de la página.
- En desarrollo, `ng serve` reenvía `/api` a `http://localhost:8080` (`proxy.conf.json`). En Docker Compose, el mismo Nginx sirve la aplicación y reenvía la API. En los dos casos el navegador habla con un solo origen, así que el backend no necesita CORS.

## Tipos generados desde el contrato

`src/app/core/api/catalogo-api.ts` se genera con `openapi-typescript` a partir de `contratos/catalogo.openapi.yaml`. **No se edita a mano**: si cambia el contrato, se corre `npm run gen:api` y se versiona el resultado. El job `frontend` de CI lo regenera y falla si queda distinto al versionado.

`src/app/core/api/modelos.ts` solo les pone nombres cortos a esos tipos. Las tablas de textos de `src/app/shared/etiquetas.ts` exigen todos los valores de cada enumeración, así que un valor nuevo en el contrato rompe el build hasta ponerle nombre.

El archivo generado está versionado para que la imagen de Docker se construya solo con la carpeta `frontend/`, sin el contrato.

## Estructura

```
src/app
├── core/                    lo que existe una sola vez en la aplicación
│   ├── api/                 tipos generados, servicios HTTP del catálogo, del prestador y de opiniones
│   ├── errores/             interceptor que traduce el problem+json a un mensaje para el usuario
│   ├── identidad/           usuario demo elegido e interceptor que lo envía a /prestador/** o a /turista/**
│   └── avisos/              mensajes breves de confirmación
├── shared/                  piezas reutilizables: mapa, iconos, estrellas, selector del usuario demo, paginación, diálogo de confirmación, pipes y textos
└── features/
    ├── turista/             catálogo (/), ficha con opiniones (/ofertas/:id) y mis opiniones (/mis-opiniones)
    └── prestador/           panel, mis ofertas, detalle y formulario de publicar o editar
```

| Ruta | Pantalla |
|---|---|
| `/` | Catálogo con filtros en los query params y paginación de 12 |
| `/ofertas/:id` | Ficha con zona, perfil del prestador, mapa y opiniones; con un turista elegido, "Dejar una opinión" |
| `/mis-opiniones` | Opiniones del turista elegido, con su estado y el motivo si fue rechazada |
| `/prestador/ofertas` | Mis ofertas, con su estado y las acciones |
| `/prestador/ofertas/nueva` | Publicar una oferta |
| `/prestador/ofertas/:id` | Detalle: versión publicada y versión pendiente |
| `/prestador/ofertas/:id/editar` | Editar una oferta |

## Diseño

Los colores, la tipografía y las piezas comunes (tarjetas, etiquetas, botones, campos, alertas) están en `src/styles.css`, como variables y clases globales; cada pantalla solo agrega su propia disposición. Los iconos son SVG en línea (`shared/icono`), sin imágenes externas. La API no tiene fotos de ofertas, así que la portada de cada oferta se dibuja con el icono de su tipo.

- **Tipografía.** Manrope en los títulos (`h1` a `h3`, pesos 700 y 800) e Inter en el texto. Las dos se cargan desde `@fontsource` en la lista `styles` de `angular.json`, y las variables `--fuente-titulos` y `--fuente-texto` las nombran.
- **Variables en `:root`.** Colores de marca (`--acento`, `--enlace`, `--profundo`), estados (`--peligro`, `--aviso-*`, `--exito-*`, `--info-*`), radios (`--radio-sm` a `--radio-pildora`) y sombras (`--sombra-1` a `--sombra-3`). `--enlace` (#0b7373) es el turquesa que lleva texto blanco; el turquesa de marca (`--acento`) va en fondos grandes e iconos.
- **Contraste.** El texto blanco va solo sobre `--enlace` o más oscuro. Los campos conservan el borde `--borde-control` para que se distingan del fondo (contraste 3:1), y el contorno de `:focus-visible` se mantiene en todos los controles.
- **Cabecera y pie.** Están en `src/app/app.html` y `app.css`. La cabecera no es fija al hacer scroll para no tapar la tarjeta lateral de la ficha.

## Identidad provisional

No hay inicio de sesión. La cabecera tiene el selector **Usuario demo** con los dos turistas (Laura y Andrés) y los cinco prestadores de los datos de demostración; el elegido se guarda en `localStorage`. Está en la cabecera y no en el panel del prestador porque el turista lo necesita en la ficha y en "Mis opiniones".

Un interceptor agrega la identidad solo a las rutas que la exigen y solo si el usuario tiene ese rol: `X-Usuario-Id`, `X-Rol: PRESTADOR` y `X-Tipo-Prestador` a `/api/catalogo/prestador/**`, y `X-Usuario-Id` y `X-Rol: TURISTA` a `/api/catalogo/turista/**`. Con un turista elegido, el panel del prestador pide elegir un prestador; con un prestador elegido, la ficha pide elegir un turista para opinar. Cuando exista el JWT del núcleo, se reemplazan `core/identidad` y el selector.

## Errores de la API

El interceptor de errores lee el `application/problem+json` y traduce su `codigo` a un mensaje (`core/errores/error-api.ts`). El código nunca se muestra. Con `DATO_INVALIDO`, la API nombra el campo en `detail` (`campo: detalle`) y el formulario marca ese campo.

## Supuestos

Lo que el contrato no cubre se resolvió en el frontend y está marcado con `[SUPUESTO]` en el código:

- **Detalle de una oferta propia.** No hay un `GET` de una sola oferta del prestador: se pide la lista y se busca por `id`.
- **Edición.** La API no devuelve el contenido completo de una oferta propia. El formulario se llena con la ficha pública (`GET /ofertas/{id}`), es decir, con la versión publicada. Por eso una oferta que solo tiene versión pendiente no ofrece "Editar", y tampoco se puede editar una oferta publicada que en ese momento no es visible en el catálogo.
- **Unidad del precio en "Mis ofertas".** La lista de ofertas propias no trae `unidadPrecio`; se toma de la tabla por tipo de oferta de DC-22.
- **Estado de la oferta.** Se calcula con `retirada`, `publicada` y `pendiente`. Si cumple más de una definición manda este orden: retirada, rechazada, publicada con cambios en revisión, en moderación, publicada.

## Docker

`Dockerfile` compila la aplicación con Node y la copia a `nginx:1.27-alpine`. La configuración de Nginx no va en la imagen: Docker Compose monta `infra/nginx/nginx.conf`.
