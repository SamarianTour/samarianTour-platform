# Samarian Tour

Plataforma de gestión sostenible del turismo en Santa Marta. Monorepo Maven multimódulo con arquitectura basada en servicios (`nucleo`, `catalogo`, `recomendacion`) y arquitectura hexagonal dentro de cada módulo de cada servicio (ADR-02).

> En construcción. El prototipo es el servicio de catálogo: búsqueda y ficha de ofertas, zonas, publicación, edición, retiro y listado de ofertas por el prestador, y moderación por avisos del núcleo. Guarda los datos en MongoDB (perfil `mongo`) o en memoria (perfil `memoria`). El frontend en Angular cubre esas funciones para el turista y el prestador; el núcleo y la recomendación están pendientes.

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `servicios/catalogo` | Servicio de catálogo (MongoDB). Módulos `catalogo` y `opiniones`, más `compartido`. |
| `servicios/nucleo` | Núcleo transaccional (PostgreSQL). Esqueleto. |
| `servicios/recomendacion` | Recomendación (sin base de datos propia). Esqueleto. |
| `libs/seguridad-jwt` | Validación del JWT RS256 y roles, compartida por los tres servicios. Pendiente. |
| `contratos/` | Contratos OpenAPI (API first). Está el del catálogo: `catalogo.openapi.yaml`. |
| `frontend/` | Aplicación Angular: catálogo y ficha para el turista, y panel del prestador. Ver [frontend/README.md](frontend/README.md). |
| `infra/` | Docker Compose, Nginx y precarga de MongoDB. |
| `docs/` | Documento de diseño, diagramas, [decisiones del prototipo](docs/decisiones-catalogo-ofertas.md), [flujo del catálogo](docs/flujo-catalogo.md) y ADR ([ADR-05](docs/adr/ADR-05-control-de-concurrencia.md)). |

## Estructura de paquetes de un servicio

Cada servicio se divide en módulos de dominio y cada módulo es hexagonal por dentro:

```
co.edu.unimagdalena.samariantour.catalogo
├── CatalogoApplication
├── compartido/                 transversal; no importa ningún módulo
├── catalogo/                   módulo
│   ├── api/                    (solo si otro módulo lo usa) lo único visible para otros módulos
│   ├── dominio/
│   │   ├── modelo/             Java puro
│   │   ├── politica/
│   │   ├── excepcion/
│   │   └── puerto/
│   │       ├── entrada/        un caso de uso por interfaz (+ comando/)
│   │       └── salida/
│   ├── aplicacion/             implementa los casos de uso
│   └── infraestructura/
│       ├── config/             crea los servicios de aplicación y los conecta con los adaptadores
│       ├── entrada/rest/       controladores, dto/, mapper/
│       ├── salida/memoria/     adaptadores en memoria (perfil memoria)
│       ├── salida/demo/        datos de demostración de Santa Marta
│       └── salida/persistencia/ adaptadores MongoDB (perfil mongo), document/ y mapper/
└── opiniones/                  módulo (fuera del alcance del prototipo)
```

Las reglas se verifican con ArchUnit en cada build (`ArquitecturaTest`):

1. `dominio` no importa Spring, Jakarta, Jackson, MongoDB, `aplicacion` ni `infraestructura`.
2. `aplicacion` solo depende de `dominio` y de `compartido.concurrencia` (se permiten `@Service` y `@Transactional`; ADR-05).
3. Ningún paquete depende de `infraestructura`.
4. Un módulo solo importa el paquete `api` de otro módulo.
5. Los módulos no forman ciclos.
6. `compartido` no importa ningún módulo.
7. Los controladores solo llaman puertos de entrada; nunca repositorios ni puertos de salida.
8. Los documentos Mongo y los repositorios viven en `infraestructura/salida/persistencia`.
9. `compartido.concurrencia` es Java puro, porque lo usa `aplicacion` (ADR-05).

## Requisitos

Java 21, Maven 3.9 y Docker (las pruebas de integración usan Testcontainers). Para trabajar en el frontend, Node.js 24 (o 22.22 en adelante) con npm; para levantar el piloto con Docker Compose no hace falta Node.

## Compilar y probar

```bash
mvn test      # unitarias, controladores, contrato y ArchUnit (no necesita Docker)
mvn verify    # además, las de integración (*IT) con MongoDB en Testcontainers
```

`ContratoCatalogoTest` compara las rutas de los controladores y los campos de los DTO con `contratos/catalogo.openapi.yaml`: si difieren, el build falla.

## Perfiles del catálogo

| Perfil | Qué activa | Cuándo |
|---|---|---|
| `memoria` | Adaptadores en la memoria del proceso, con datos de demostración de Santa Marta | Por defecto, sin Docker |
| `mongo` | Adaptadores de MongoDB; crea los índices al arrancar | Docker Compose y pruebas de integración (`mvn verify`) |
| `demo` | Los headers que sustituyen al token del prestador y, junto con `mongo`, la carga de los datos de demostración | Nunca por defecto; solo en la demostración |

**Identidad provisional.** Mientras no exista `libs/seguridad-jwt`, el usuario se identifica con los mismos datos que traerá el JWT, en headers: `X-Usuario-Id` (UUID), `X-Rol` (`TURISTA`, `PRESTADOR` o `ADMINISTRADOR`) y, solo para el rol `PRESTADOR`, `X-Tipo-Prestador` (`AGENCIA_OPERADORA`, `GUIA_INDEPENDIENTE`, `ALOJAMIENTO` o `RESTAURANTE`). Cada prefijo exige un rol: `/prestador/**` PRESTADOR, `/admin/**` ADMINISTRADOR y `/turista/**` TURISTA; sin identidad responden 401 `NO_AUTENTICADO` y con otro rol 403 `ACCESO_DENEGADO`. Cualquiera puede enviar los headers, así que solo se aceptan con el perfil `demo`. Los reemplaza el JWT que emitirá el núcleo, y entonces solo cambia `compartido/seguridad`.

## Levantar el piloto

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

Nginx queda en `http://localhost:8080`: sirve el frontend en `/` y enruta `/api/catalogo/**` hacia las 2 réplicas del catálogo. La imagen de Nginx se construye con `frontend/Dockerfile`, que compila la aplicación Angular. Cada respuesta trae el header `X-Instancia` con el hostname del contenedor que la atendió, y los errores salen como `application/problem+json` (RFC 7807). Las rutas `/api/catalogo/internal/**` y `/api/catalogo/actuator/**` no se exponen hacia afuera.

Compose activa los perfiles `mongo` y `demo`: los datos viven en MongoDB y las dos réplicas comparten el estado. Al arrancar, el catálogo carga los datos de demostración en las colecciones que estén vacías; las dos réplicas pueden hacerlo a la vez sin duplicar nada. Nginx arranca cuando el catálogo responde su healthcheck (`/actuator/health`).

Para volver a los datos iniciales hay que borrar el volumen de MongoDB:

```bash
docker compose -f infra/docker-compose.yml down -v
```

Actualizar solo el catálogo sin detener lo demás:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps catalogo
```

Actualizar solo el frontend:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps nginx
```

## Frontend en desarrollo

Con el piloto levantado, el frontend también se puede correr fuera de Docker, con recarga al guardar:

```bash
cd frontend
npm ci
npm start        # http://localhost:4200
```

`ng serve` reenvía `/api` a `http://localhost:8080` (`frontend/proxy.conf.json`), así que el navegador siempre habla con su mismo origen y no hace falta CORS en el backend. El resto (tipos generados desde el contrato, pruebas, estructura) está en [frontend/README.md](frontend/README.md).

## Demostración por la interfaz

Con el piloto levantado, abre `http://localhost:8080`.

1. **Catálogo.** La página inicial lista las ofertas visibles. Filtra por tipo, zona, tipo de actividad, precio, esfuerzo físico máximo ("hasta el nivel") y accesibilidad. Los filtros quedan en la URL: `http://localhost:8080/?tipo=ACTIVIDAD&zonaId=a00000000000000000000005` abre las actividades del Parque Tayrona. Si nada coincide aparece "Limpiar filtros".
2. **Ficha.** Entra a "Tayrona: mar y selva" (un paquete): muestra sus actividades con enlace, el esfuerzo calculado, la zona con sus buenas prácticas, el perfil del prestador y el mapa con el enlace "Cómo llegar". Una oferta que no es visible, como `http://localhost:8080/ofertas/b00000000000000000000016`, muestra "Esta oferta ya no está disponible".
3. **Publicar.** En "Soy prestador" elige el prestador demo **Guía** y entra a "Publicar oferta". Llena la actividad, pon el pin en el mapa y envíala: vuelve a "Mis ofertas" con el mensaje "Enviamos tu oferta a moderación (versión 1)" y la oferta queda "En moderación".
4. **Aprobar por consola.** La moderación es del núcleo, que todavía no existe: el aviso se envía a mano desde dentro de la red de Docker. El identificador de la oferta está en la URL de "Ver detalle" (`/prestador/ofertas/<id>`):

   ```bash
   ID=<id de la oferta>
   docker compose -f infra/docker-compose.yml exec -T nginx \n     curl -s -X POST http://catalogo:8080/api/catalogo/internal/avisos \n     -H "Content-Type: application/json" \n     -d "{\"idAviso\":\"demo-$ID\",\"tipo\":\"MODERACION\",\"ofertaId\":\"$ID\",\"numeroVersion\":1,\"decision\":\"APROBAR\"}"
   # {"resultado":"APLICADO"}
   ```

   Al recargar "Mis ofertas" la oferta está "Publicada" y ya aparece en el catálogo. Para mostrar un rechazo, usa `\"decision\":\"RECHAZAR\",\"motivo\":\"Falta el punto de encuentro\"`: la oferta queda "Rechazada" con su motivo.
5. **Editar.** En "Mis ofertas", "Editar" abre el formulario con los datos publicados. Cambia el precio y guarda: la oferta pasa a "Publicada con cambios en revisión" y en "Ver detalle" se ven lado a lado la versión publicada (1) y la pendiente (2). El turista sigue viendo la versión 1 hasta que se apruebe la 2 con otro aviso (`numeroVersion: 2` y otro `idAviso`).
6. **Retirar.** "Retirar" pide confirmación en un diálogo propio. Al confirmar, la oferta queda "Retirada", sin "Editar", y su ficha pública responde "Esta oferta ya no está disponible".
7. **Errores.** Con el prestador demo **Agencia sin RNT**, publicar muestra que el RNT no está activo. Con **Hotel** o **Restaurante** el formulario solo ofrece el tipo de oferta que ese prestador puede publicar.

La identidad del prestador es provisional: el selector "Prestador demo" sustituye al inicio de sesión mientras no exista el JWT del núcleo.

## Demostración paso a paso por consola

La misma demostración contra la API, sin la interfaz. Los comandos están en `bash` (en Windows, Git Bash). Los datos de demostración son inventados: 7 zonas, 5 prestadores y 23 ofertas de los cuatro tipos.

```bash
BASE=http://localhost:8080/api/catalogo
```

### 1. Búsqueda con filtros, repartida entre las réplicas

```bash
# El header X-Instancia muestra qué réplica respondió: Nginx reparte las peticiones entre las dos, sin un orden fijo
for i in 1 2 3 4 5 6 7 8; do curl -s -o /dev/null -D - "$BASE/ofertas" | grep -i '^x-instancia'; done

# Búsqueda por texto: sin mayúsculas ni tildes y con palabras incompletas ("cafe" encuentra "café")
curl -s "$BASE/ofertas?texto=cafe"
curl -s "$BASE/ofertas?texto=buce"

# Actividades en el Parque Tayrona
curl -s "$BASE/ofertas?tipo=ACTIVIDAD&zonaId=a00000000000000000000005"

# Lo que puede hacer una persona con movilidad reducida: esfuerzo bajo y accesible en silla de ruedas
curl -s "$BASE/ofertas?nivelEsfuerzoMaximo=BAJO&accesibleSillaRuedas=true"

# Buceo y snorkel: actividades de ese tipo y paquetes que incluyen alguna
curl -s "$BASE/ofertas?tipoActividad=BUCEO_SNORKEL"

# Un filtro inválido responde 400 con el código DATO_INVALIDO
curl -s "$BASE/ofertas?tipo=HOTEL"
```

### 2. Ficha y zonas

```bash
# Paquete: su ubicación, sus actividades, el nivel de esfuerzo calculado, la zona protegida
# y el perfil del prestador con su nombre comercial
curl -s "$BASE/ofertas/b00000000000000000000008"

# Oferta aprobada de un prestador con el RNT inactivo: no es visible, responde 404
curl -s "$BASE/ofertas/b00000000000000000000016"

curl -s "$BASE/zonas"
```

### 3. Publicar, aprobar y ver la oferta en la búsqueda

Los datos están en MongoDB, así que el flujo funciona con las dos réplicas: lo que escribe una lo ve la otra.

Publicar una actividad como el guía independiente de la demostración:

```bash
ID=$(curl -s -X POST "$BASE/prestador/ofertas" \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: 20000000-0000-0000-0000-000000000002" \
  -H "X-Rol: PRESTADOR" \
  -H "X-Tipo-Prestador: GUIA_INDEPENDIENTE" \
  -d '{
        "tipo": "ACTIVIDAD",
        "nombre": "Caminata nocturna en Minca",
        "descripcion": "Recorrido para observar ranas e insectos al caer la noche",
        "precioReferencia": 75000,
        "zonaId": "a00000000000000000000007",
        "capacidadMaxima": 6,
        "tipoActividad": "AVISTAMIENTO_FAUNA",
        "nivelEsfuerzo": "BAJO",
        "horariosSalida": ["18:30"],
        "puntoEncuentro": "Plaza principal de Minca",
        "duracionMinutos": 120,
        "modalidad": "CAMINATA",
        "idiomasAtencion": ["es", "en"]
      }' | sed -E 's/^\{"id":"([^"]+)".*/\1/')
echo "$ID"
```

La oferta nace con su versión 1 en moderación y todavía no es visible:

```bash
curl -s "$BASE/ofertas/$ID"                         # 404 OFERTA_NO_ENCONTRADA
```

El núcleo avisa la aprobación por una ruta interna. Desde afuera Nginx responde 404; el aviso se envía desde dentro de la red de Docker:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/internal/avisos"      # 404

docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s -X POST http://catalogo:8080/api/catalogo/internal/avisos \
  -H "Content-Type: application/json" \
  -d "{\"idAviso\":\"demo-$ID\",\"tipo\":\"MODERACION\",\"ofertaId\":\"$ID\",\"numeroVersion\":1,\"decision\":\"APROBAR\"}"
# {"resultado":"APLICADO"}; si se repite el mismo aviso, {"resultado":"DUPLICADO"}, aunque llegue a la otra réplica
```

Ahora aparece en la ficha y en la búsqueda:

```bash
curl -s "$BASE/ofertas/$ID"
curl -s "$BASE/ofertas?zonaId=a00000000000000000000007&tipoActividad=AVISTAMIENTO_FAUNA"
```

### 4. Ofertas propias y retiro

```bash
# Las ofertas del guía en cualquier estado, con su versión publicada y la pendiente
curl -s "$BASE/prestador/ofertas" \
  -H "X-Usuario-Id: 20000000-0000-0000-0000-000000000002" \
  -H "X-Rol: PRESTADOR" \
  -H "X-Tipo-Prestador: GUIA_INDEPENDIENTE"

# Retirar la oferta recién publicada: 204. El retiro es definitivo
curl -s -o /dev/null -w '%{http_code}\n' -X PATCH "$BASE/prestador/ofertas/$ID/retiro" \
  -H "X-Usuario-Id: 20000000-0000-0000-0000-000000000002" \
  -H "X-Rol: PRESTADOR" \
  -H "X-Tipo-Prestador: GUIA_INDEPENDIENTE"

# Ya no es visible para el turista (404), pero sigue en las ofertas propias como "retirada": true
curl -s "$BASE/ofertas/$ID"
```

### 5. Consultas internas del núcleo y de la recomendación

El núcleo consulta la oferta al crear una solicitud, y la recomendación lee las ofertas visibles. Como los avisos, solo responden dentro de la red de Docker:

```bash
# Lo que la solicitud copia (nombre, tipo, zona, prestador) y lo que valida (horarios de salida y capacidad)
docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s http://catalogo:8080/api/catalogo/internal/ofertas/b00000000000000000000001

# Oferta aprobada de un prestador con el RNT inactivo: no se puede pedir, 404
docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s http://catalogo:8080/api/catalogo/internal/ofertas/b00000000000000000000016

# Paquetes visibles del Parque Tayrona, con la alta afluencia de la zona, los tipos de actividad y el esfuerzo
docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s "http://catalogo:8080/api/catalogo/internal/ofertas?zonaId=a00000000000000000000005&tipo=PAQUETE"
```

### 6. Rutas que no se exponen

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/internal/avisos"      # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/internal/ofertas"             # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/actuator"                     # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/actuator/health"              # 404
```

Otros casos que se pueden mostrar:

- **Sin identidad:** el mismo `POST` sin los headers responde 401 `NO_AUTENTICADO`.
- **Rol equivocado:** el mismo `POST` con `X-Rol: TURISTA` responde 403 `ACCESO_DENEGADO`.
- **RNT inactivo:** con `X-Usuario-Id: 50000000-0000-0000-0000-000000000005`, `X-Rol: PRESTADOR` y `X-Tipo-Prestador: AGENCIA_OPERADORA` responde 403 `RNT_INACTIVO`.
- **Tipo no permitido:** el guía que publica un `ALOJAMIENTO` recibe 403 `TIPO_OFERTA_NO_PERMITIDO`.
- **Edición:** antes de retirarla, un `PUT $BASE/prestador/ofertas/$ID` con otro precio crea la versión 2 en moderación; la ficha sigue mostrando la versión aprobada hasta que llegue su aviso.
- **Oferta retirada:** después del retiro, ese mismo `PUT` responde 409 `OFERTA_NO_EDITABLE`.

## Cómo contribuir

El flujo de ramas (GitFlow), los pull requests y la guía de mensajes de commit están en [CONTRIBUTING.md](CONTRIBUTING.md).
