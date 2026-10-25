# Samarian Tour

Plataforma de gestión sostenible del turismo en Santa Marta. Monorepo Maven multimódulo con arquitectura basada en servicios (`nucleo`, `catalogo`, `recomendacion`) y arquitectura hexagonal dentro de cada módulo de cada servicio (ADR-02).

> En construcción. El prototipo es el servicio de catálogo: búsqueda y ficha de ofertas, zonas y su gestión por el administrador, publicación, edición, retiro y listado de ofertas por el prestador, las fotos de sus ofertas, su perfil público con fotos, opiniones de los turistas, y moderación por avisos del núcleo. Guarda los datos en MongoDB (perfil `mongo`) o en memoria (perfil `memoria`). El servicio de recomendación sugiere de 3 a 5 ofertas del catálogo, cada una explicada, con un motor por reglas o con IA. El frontend en Angular cubre esas funciones para el turista y el prestador, salvo la gestión del perfil, la subida de fotos de las ofertas y la gestión de zonas, que por ahora son solo por la API, y la moderación, que el administrador hace en el núcleo. Del núcleo está la base (PostgreSQL con Flyway, la identidad provisional y la bandeja de salida que envía los avisos al catálogo) y la moderación de ofertas y opiniones; sus demás módulos están pendientes.


## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `servicios/catalogo` | Servicio de catálogo (MongoDB). Módulos `catalogo` y `opiniones`, más `compartido`. |
| `servicios/nucleo` | Núcleo transaccional (PostgreSQL). Base del servicio y bandeja de salida hacia el catálogo; módulos pendientes. Ver [servicios/nucleo/README.md](servicios/nucleo/README.md). |
| `servicios/recomendacion` | Recomendación (sin base de datos propia): ofertas explicadas, por reglas o con IA. Ver [su README](servicios/recomendacion/README.md). |
| `libs/seguridad-jwt` | Validación del JWT RS256 y roles, compartida por los tres servicios. Pendiente. |
| `contratos/` | Contratos OpenAPI (API first): `catalogo.openapi.yaml`, `nucleo.openapi.yaml` y `recomendacion.openapi.yaml`. |
| `frontend/` | Aplicación Angular: catálogo y ficha para el turista, y panel del prestador. Ver [frontend/README.md](frontend/README.md). |
| `infra/` | Docker Compose, Nginx, precarga de MongoDB y la prueba de carga de la búsqueda (`infra/carga`). |
| `docs/` | Documento de diseño, diagramas, [decisiones del catálogo](docs/decisiones-catalogo-ofertas.md), [del núcleo](docs/decisiones-nucleo.md) y [de la recomendación](docs/decisiones-recomendacion.md), [flujo del catálogo](docs/flujo-catalogo.md) y ADR ([ADR-05](docs/adr/ADR-05-control-de-concurrencia.md)). |

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
│       ├── entrada/programada/ tareas programadas: la limpieza de fotos sin uso
│       ├── salida/memoria/     adaptadores en memoria (perfil memoria)
│       ├── salida/demo/        datos y fotos de demostración de Santa Marta, y el catálogo sintético de carga
│       ├── salida/persistencia/ adaptadores MongoDB (perfil mongo), document/ y mapper/
│       └── salida/almacenamiento/ fotos en MinIO (perfil minio) o en una carpeta local
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
mvn verify    # además, las de integración (*IT) con MongoDB y MinIO en Testcontainers
```

`ContratoCatalogoTest` y `ContratoRecomendacionTest` comparan las rutas de los controladores y los campos de los DTO con su contrato en `contratos/`: si difieren, el build falla. Ninguna prueba llama a un proveedor de IA ni al catálogo real.

## Perfiles del catálogo

| Perfil | Qué activa | Cuándo |
|---|---|---|
| `memoria` | Adaptadores en la memoria del proceso, con datos de demostración de Santa Marta | Por defecto, sin Docker |
| `mongo` | Adaptadores de MongoDB; crea los índices al arrancar | Docker Compose y pruebas de integración (`mvn verify`) |
| `minio` | Las fotos en MinIO (DC-39); sin él, en una carpeta local (`catalogo.fotos.carpeta`, por defecto en la carpeta temporal) | Docker Compose |
| `demo` | Los headers que sustituyen al token del prestador y, junto con `mongo`, la carga de los datos de demostración | Nunca por defecto; solo en la demostración |
| `carga` | Junto con `mongo`, agrega 5.000 ofertas sintéticas después de los datos de demostración (`catalogo.carga.ofertas`, DC-48) | Solo en la prueba de carga |

**Identidad provisional.** Mientras no exista `libs/seguridad-jwt`, el usuario se identifica con los mismos datos que traerá el JWT, en headers: `X-Usuario-Id` (UUID), `X-Rol` (`TURISTA`, `PRESTADOR` o `ADMINISTRADOR`) y, solo para el rol `PRESTADOR`, `X-Tipo-Prestador` (`AGENCIA_OPERADORA`, `GUIA_INDEPENDIENTE`, `ALOJAMIENTO` o `RESTAURANTE`). Cada prefijo exige un rol: `/prestador/**` PRESTADOR, `/admin/**` ADMINISTRADOR y `/turista/**` TURISTA; sin identidad responden 401 `NO_AUTENTICADO` y con otro rol 403 `ACCESO_DENEGADO`. Cualquiera puede enviar los headers, así que solo se aceptan con el perfil `demo`. Los reemplaza el JWT que emitirá el núcleo, y entonces solo cambia `compartido/seguridad`.

## Levantar el piloto

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

Nginx queda en `http://localhost:8080`: sirve el frontend en `/`, enruta `/api/catalogo/**` hacia las 2 réplicas del catálogo, `/api/nucleo/**` hacia las 2 del núcleo y `/api/recomendacion/**` hacia la recomendación. La imagen de Nginx se construye con `frontend/Dockerfile`, que compila la aplicación Angular. Cada respuesta trae el header `X-Instancia` con el hostname del contenedor que la atendió, y los errores salen como `application/problem+json` (RFC 7807). Las rutas `internal/**` del catálogo y del núcleo y las `actuator/**` de los tres servicios no se exponen hacia afuera.

Compose activa los perfiles `mongo`, `minio` y `demo`: los datos viven en MongoDB y las fotos en MinIO, y las dos réplicas comparten el estado. MinIO no tiene puertos publicados; el navegador pide las fotos al catálogo. Al arrancar, el catálogo carga los datos de demostración en las colecciones que estén vacías; las dos réplicas pueden hacerlo a la vez sin duplicar nada. El núcleo guarda sus datos en PostgreSQL, sin puertos publicados; al arrancar, Flyway crea o actualiza el esquema (DN-01), y cada 15 s el núcleo envía al catálogo los avisos de su bandeja de salida (DN-06). Nginx arranca cuando el catálogo y el núcleo responden su healthcheck (`/actuator/health`). Cada hora una tarea borra las fotos que nadie usa; un candado en MongoDB (ShedLock) hace que la corra una sola réplica (DC-47).

Para volver a los datos iniciales hay que borrar los volúmenes de MongoDB, MinIO y PostgreSQL. También hace falta con una base creada antes del perfil con fotos (DC-39) o de las fotos de las ofertas (DC-47):

```bash
docker compose -f infra/docker-compose.yml down -v
```

Actualizar solo el catálogo sin detener lo demás:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps catalogo
```

Actualizar solo el núcleo:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps nucleo
```

Actualizar solo la recomendación:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps recomendacion
```

Actualizar solo el frontend:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps nginx
```

### Motor de recomendación: reglas o IA

Sin configurar nada, la recomendación responde con el **motor por reglas** (`"motor": "REGLAS"`): elige por los criterios explícitos y explica cada oferta con una plantilla (DR-01, DR-03 de [las decisiones de la recomendación](docs/decisiones-recomendacion.md)).

Para usar el **motor con IA** (Groq, plan gratis), crea `infra/.env` a partir de la plantilla. Ese archivo no se versiona: la key nunca va al repositorio.

```bash
cp infra/.env.example infra/.env
# En infra/.env: IA_HABILITADA=true y GROQ_API_KEY=<tu key de https://console.groq.com/keys>
docker compose -f infra/docker-compose.yml up -d recomendacion
docker compose -f infra/docker-compose.yml logs recomendacion | grep "motor de recomendacion principal"
# ... motor de recomendacion principal: IA
```

Desde ahí las respuestas dicen `"motor": "IA"`. Si Groq tarda más de 6 s, responde 429 (límite del plan) o 5xx, o su respuesta no pasa la validación, esa consulta la responde el motor por reglas sin error para el turista; tras 3 fallas seguidas, el corte de circuito va directo a reglas por 60 s (DR-05, DR-06). El log del servicio dice el motor, la latencia y la causa de cada falla, nunca el texto del turista ni la key.

**Cambiar de proveedor sin recompilar (QAS-06).** Es el mismo adaptador para cualquier API compatible con la de OpenAI: solo cambian la URL, el modelo y la key en `infra/.env`. Con Ollama en el mismo equipo:

```bash
# En infra/.env:
#   IA_HABILITADA=true
#   IA_URL=http://host.docker.internal:11434/v1
#   IA_MODELO=llama3.2
#   IA_ESFUERZO_RAZONAMIENTO=
#   GROQ_API_KEY=ollama
docker compose -f infra/docker-compose.yml up -d recomendacion    # recrea el contenedor con la misma imagen
```

### Fuente de las alertas: IDEAM o simulada

Las alertas del destino (RF-28) salen por defecto de la capa de alertas hidrológicas vigentes del IDEAM, consultada con las coordenadas de cada zona al arrancar y cada 8 horas (DR-14 a DR-16). El contenedor necesita salida a internet hacia `visualizador.ideam.gov.co`. Para la demo sin internet, o para mostrar todos los niveles y tipos (cierre del Tayrona, deslizamiento en Minca, oleaje en Taganga), está la fuente simulada, cuyas alertas dicen "(simulado)" en la fuente:

```bash
# En infra/.env: ALERTAS_FUENTE=simulada  (o, solo esta vez, en la misma línea:)
ALERTAS_FUENTE=simulada docker compose -f infra/docker-compose.yml up -d recomendacion
docker compose -f infra/docker-compose.yml logs recomendacion | grep "alertas"
# ... alertas actualizadas latenciaMs=238
# Con la fuente ideam:  alertas ideam: zonas=7 conAlerta=7 fallidas=0 latenciaMs=2004
```

Si el IDEAM no responde, cada zona conserva su última lectura. Si la lectura del arranque falla (por ejemplo, sin internet), las alertas responden 503 `ALERTAS_NO_DISPONIBLES` y la pantalla omite la sección, y el servicio reintenta cada 2 minutos hasta tener una lectura; después sigue el ciclo de 8 horas (DR-16):

```bash
docker compose -f infra/docker-compose.yml logs recomendacion | grep "alertas"
# ... alertas sin actualizar: se conserva la ultima lectura buena latenciaMs=...     (el arranque)
# ... alertas sin lectura todavia: se reintenta en el proximo ciclo latenciaMs=...   (cada 2 minutos)
# ... alertas actualizadas en el reintento latenciaMs=...
```

 Si la capa cambia de dirección, va en `IDEAM_ALERTAS_URL` (ver `infra/.env.example`).

## Prueba de carga de la búsqueda (QAS-07)

500 usuarios buscan durante 10 minutos con filtros de tipo, zona y precio sobre 5.000 ofertas, y k6 compara el resultado con QAS-07 (p95 ≤ 2 s en el servidor) y con la parte de QAS-02 que le toca al catálogo (menos de 1 % de errores, al menos 50 búsquedas por segundo). Corre como un proyecto aparte, `samarian-carga`, con sus propios volúmenes: no toca los datos del piloto. Los dos publican el puerto 8080, así que hay que detener el piloto antes (`docker compose -f infra/docker-compose.yml stop`). El método y los resultados están en DC-48.

```bash
# Levanta el piloto con el catálogo sintético
docker compose -f infra/docker-compose.yml -f infra/docker-compose.carga.yml up -d --build --wait

# Corre la prueba: unos 12 minutos. Para una prueba corta: USUARIOS=20 DURACION=30s
docker compose -f infra/docker-compose.yml -f infra/docker-compose.carga.yml run --rm k6

# Borra todo, incluidos sus volúmenes
docker compose -f infra/docker-compose.yml -f infra/docker-compose.carga.yml down -v
```

Al final k6 imprime las búsquedas por segundo, los errores, el p95 en el cliente y, con las métricas de las dos réplicas, qué parte de las búsquedas respondió en 2 s o menos en el servidor; cada umbral sale como `CUMPLE` o `NO CUMPLE`. El resumen completo queda en `infra/carga/resultados/resumen.json`, que no se versiona.

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
3. **Publicar.** En el selector **Usuario demo** de la cabecera elige **Guía**, entra a "Soy prestador" y luego a "Publicar oferta". Llena la actividad, pon el pin en el mapa y envíala: vuelve a "Mis ofertas" con el mensaje "Enviamos tu oferta a moderación (versión 1)" y la oferta queda "En moderación".
4. **Aprobar desde el núcleo.** La interfaz del administrador todavía no existe, así que la decisión se envía con `curl` al núcleo (DN-08). El identificador de la oferta está en la URL de "Ver detalle" (`/prestador/ofertas/<id>`):

   ```bash
   ID=<id de la oferta>
   curl -s -X POST "http://localhost:8080/api/nucleo/admin/moderacion/ofertas/$ID/decision" \
     -H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" -H "X-Rol: ADMINISTRADOR" \
     -H "Content-Type: application/json" -d '{"numeroVersion": 1, "decision": "APROBAR"}'
   ```

   El núcleo guarda la decisión y su bandeja la lleva al catálogo en los siguientes 15 s: al recargar "Mis ofertas" la oferta está "Publicada" y ya aparece en el catálogo. Para mostrar un rechazo, usa `'{"numeroVersion": 1, "decision": "RECHAZAR", "motivo": "Falta el punto de encuentro"}'`: la oferta queda "Rechazada" con su motivo.
5. **Editar.** En "Mis ofertas", "Editar" abre el formulario con los datos publicados. Cambia el precio y guarda: la oferta pasa a "Publicada con cambios en revisión" y en "Ver detalle" se ven lado a lado la versión publicada (1) y la pendiente (2). El turista sigue viendo la versión 1 hasta que el administrador apruebe la 2 (`"numeroVersion": 2`).
6. **Retirar.** "Retirar" pide confirmación en un diálogo propio. Al confirmar, la oferta queda "Retirada", sin "Editar", y su ficha pública responde "Esta oferta ya no está disponible".
7. **Errores.** Con el usuario demo **Agencia sin RNT**, publicar muestra que el RNT no está activo. Con **Hotel** o **Restaurante** el formulario solo ofrece el tipo de oferta que ese prestador puede publicar.

8. **Leer opiniones.** La ficha de "Caminata a Cabo San Juan del Guía" muestra el promedio (4,7), el total y las opiniones aprobadas, de la más reciente a la más antigua; la rechazada de la precarga no aparece.
9. **Opinar.** En **Usuario demo** elige a **Laura**, que ya pagó el snorkel en Playa Cristal. En su ficha, en "Dejar una opinión", elige las estrellas, escribe el comentario y envíalo: aparece "Tu opinión quedó en moderación". En "Mis opiniones" la opinión está "En moderación". Si Laura intenta opinar sobre una oferta que no pagó, la ficha responde "Solo puedes opinar sobre ofertas que hayas pagado"; si vuelve a opinar sobre la misma, "Ya opinaste sobre esta oferta".
10. **Aprobar la opinión desde el núcleo.** Como con las ofertas, la decisión se envía con `curl` al núcleo; los comandos están en el paso 8 de la demostración por consola. Al recargar, la opinión aparece en la ficha y en "Mis opiniones" está "Publicada".

La identidad es provisional: el selector "Usuario demo" de la cabecera, con dos turistas y cinco prestadores, sustituye al inicio de sesión mientras no exista el JWT del núcleo.

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

# Zonas activas, y la ficha de una zona con sus buenas prácticas
curl -s "$BASE/zonas"
curl -s "$BASE/zonas/a00000000000000000000005"
```

### 3. Publicar, aprobar y ver la oferta en la búsqueda

Los datos están en MongoDB, así que el flujo funciona con las dos réplicas: lo que escribe una lo ve la otra.

Publicar una actividad como el guía independiente de la demostración. Primero sube su foto: una oferta lleva de 1 a 6 (DC-47).

```bash
GUIA=(-H "X-Usuario-Id: 20000000-0000-0000-0000-000000000002" -H "X-Rol: PRESTADOR" -H "X-Tipo-Prestador: GUIA_INDEPENDIENTE")

# Cualquier foto JPG, PNG o WebP de hasta 5 MB; responde 201 con su id y la dirección de cada tamaño
FOTO=$(curl -s -X POST "$BASE/prestador/fotos" "${GUIA[@]}" -F "foto=@mi-foto.jpg" | sed -E 's/^\{"id":"([^"]+)".*/\1/')

ID=$(curl -s -X POST "$BASE/prestador/ofertas" "${GUIA[@]}" \
  -H "Content-Type: application/json" \
  -d '{
        "tipo": "ACTIVIDAD",
        "nombre": "Caminata nocturna en Minca",
        "descripcion": "Recorrido para observar ranas e insectos al caer la noche",
        "precioReferencia": 75000,
        "zonaId": "a00000000000000000000007",
        "capacidadMaxima": 6,
        "fotos": ["'"$FOTO"'"],
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

El administrador la aprueba en el núcleo (DN-08). El núcleo comprueba con el catálogo que la versión 1 sigue en moderación, guarda la decisión y deja el aviso en su bandeja, que lo envía a la ruta interna del catálogo:

```bash
NUCLEO=http://localhost:8080/api/nucleo
ADMIN=(-H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" -H "X-Rol: ADMINISTRADOR")

curl -s -X POST "$NUCLEO/admin/moderacion/ofertas/$ID/decision" "${ADMIN[@]}" \
  -H "Content-Type: application/json" -d '{"numeroVersion": 1, "decision": "APROBAR"}'
# {"tipo":"OFERTA","moderadoId":"...","numeroVersion":1,"decision":"APROBAR","administradorId":"...","decididaEn":"..."}
```

En los siguientes 15 s aparece en la ficha y en la búsqueda:

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

# Una oferta propia completa: las dos versiones con todos sus campos, aunque nunca se haya publicado (DC-50).
# La de otro prestador responde 403 PRESTADOR_NO_AUTORIZADO
curl -s "$BASE/prestador/ofertas/$ID" \
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

### 6. Moderación

El administrador ve lo que espera su decisión: primero el paseo en lancha, que es nuevo, y después la edición de la caminata a Cabo San Juan, con la versión publicada al lado para comparar. La decisión la toma en el núcleo, como en el paso 3.

```bash
curl -s "$BASE/admin/moderacion/ofertas" \
  -H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" \
  -H "X-Rol: ADMINISTRADOR"

# El núcleo consulta la oferta que va a decidir, desde la red de Docker
docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s http://catalogo:8080/api/catalogo/internal/moderacion/ofertas/b00000000000000000000017
```

Rechazar el paseo en lancha exige motivo. El núcleo se lo notifica al prestador; por ahora la notificación solo sale en su log:

```bash
NUCLEO=http://localhost:8080/api/nucleo
ADMIN=(-H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" -H "X-Rol: ADMINISTRADOR")
LANCHA="$NUCLEO/admin/moderacion/ofertas/b00000000000000000000017/decision"

curl -s -X POST "$LANCHA" "${ADMIN[@]}" -H "Content-Type: application/json" -d '{"numeroVersion": 1, "decision": "RECHAZAR"}'
# 400 DATO_INVALIDO: "motivo: es obligatorio al rechazar"
curl -s -X POST "$LANCHA" "${ADMIN[@]}" -H "Content-Type: application/json" \
  -d '{"numeroVersion": 1, "decision": "RECHAZAR", "motivo": "Falta el punto de encuentro"}'
docker compose -f infra/docker-compose.yml logs nucleo | grep "Notificación simulada"

# Repetir el rechazo responde 200 con el registrado; aprobar después, 409 MODERACION_YA_DECIDIDA
curl -s -X POST "$LANCHA" "${ADMIN[@]}" -H "Content-Type: application/json" \
  -d '{"numeroVersion": 1, "decision": "RECHAZAR", "motivo": "Falta el punto de encuentro"}'
curl -s -X POST "$LANCHA" "${ADMIN[@]}" -H "Content-Type: application/json" -d '{"numeroVersion": 1, "decision": "APROBAR"}'

# La caminata a Cabo San Juan tiene en moderación la versión 2: decidir sobre la 1 responde 409 VERSION_NO_VIGENTE
curl -s -X POST "$NUCLEO/admin/moderacion/ofertas/b00000000000000000000001/decision" "${ADMIN[@]}" \
  -H "Content-Type: application/json" -d '{"numeroVersion": 1, "decision": "APROBAR"}'
```

Si el catálogo no responde, el núcleo no decide nada y responde 503 `CATALOGO_NO_DISPONIBLE`.

### 7. Perfil público con fotos

El guía completa su perfil y sube una foto. Se publica de inmediato, sin moderación. El catálogo guarda una versión nueva de la foto, enderezada y sin metadatos, en dos tamaños (DC-39):

```bash
GUIA=(-H "X-Usuario-Id: 20000000-0000-0000-0000-000000000002" -H "X-Rol: PRESTADOR" -H "X-Tipo-Prestador: GUIA_INDEPENDIENTE")

curl -s -X PUT "$BASE/prestador/perfil" "${GUIA[@]}" -H "Content-Type: application/json" \
  -d '{"nombreComercial":"Aves de Minca","descripcion":"Guía independiente de Minca, especializado en aves"}'

# Cualquier foto JPG, PNG o WebP de hasta 5 MB; responde 201 con la dirección de cada tamaño
curl -s -X POST "$BASE/prestador/perfil/fotos" "${GUIA[@]}" -F "foto=@mi-foto.jpg"

# La foto aparece en la ficha de las ofertas del guía, y el navegador la descarga del catálogo
curl -s "$BASE/ofertas/b0000000000000000000000a"
curl -s -o miniatura.jpg -D - "$BASE/fotos/<id>/miniatura"     # Cache-Control: max-age=31536000, immutable

curl -s -o /dev/null -w '%{http_code}\n' -X DELETE "$BASE/prestador/perfil/fotos/<id>" "${GUIA[@]}"   # 204
```

Un archivo que no es una imagen, aunque se llame `.jpg`, responde 400 `DATO_INVALIDO`; también la séptima foto.

### 8. Opiniones

Andrés es un turista de la demostración. Todavía no pagó el recorrido por el Centro Histórico, así que no puede opinar sobre él:

```bash
TURISTA=80000000-0000-0000-0000-000000000008
OFERTA=b00000000000000000000006
OPINION='{"ofertaId":"b00000000000000000000006","calificacion":5,"comentario":"Caminamos por el centro con calma y nos explicaron la historia de cada plaza."}'

curl -s -X POST "$BASE/turista/opiniones" -H "Content-Type: application/json" \
  -H "X-Usuario-Id: $TURISTA" -H "X-Rol: TURISTA" -d "$OPINION"
# 403 OPINION_NO_HABILITADA
```

Cuando una solicitud queda pagada, el núcleo habilita al turista con un aviso. Mientras el núcleo no exista, se envía a mano desde la red de Docker:

```bash
docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s -X POST http://catalogo:8080/api/catalogo/internal/opiniones/avisos \
  -H "Content-Type: application/json" \
  -d "{\"idAviso\":\"pago-$TURISTA-$OFERTA\",\"tipo\":\"HABILITACION_OPINION\",\"turistaId\":\"$TURISTA\",\"ofertaId\":\"$OFERTA\"}"
# {"resultado":"APLICADO"}
```

Ahora sí puede opinar. La opinión queda en moderación y todavía no se ve en la ficha; una segunda opinión sobre la misma oferta responde 409 `OPINION_DUPLICADA`:

```bash
OPINION_ID=$(curl -s -X POST "$BASE/turista/opiniones" -H "Content-Type: application/json" \
  -H "X-Usuario-Id: $TURISTA" -H "X-Rol: TURISTA" -d "$OPINION" | sed -E 's/^\{"id":"([^"]+)".*/\1/')
echo "$OPINION_ID"

curl -s "$BASE/ofertas/$OFERTA/opiniones"                 # sin opiniones aprobadas todavía
curl -s "$BASE/turista/opiniones" -H "X-Usuario-Id: $TURISTA" -H "X-Rol: TURISTA"   # EN_MODERACION
```

El administrador la ve en su cola, con el nombre de la oferta, y el núcleo consulta a quién notificar antes de decidir:

```bash
curl -s "$BASE/admin/moderacion/opiniones" \
  -H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" -H "X-Rol: ADMINISTRADOR"

docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s "http://catalogo:8080/api/catalogo/internal/moderacion/opiniones/$OPINION_ID"
```

El administrador decide en el núcleo, que se lo avisa al catálogo. Al aprobarla, en los siguientes 15 s aparece en la ficha, con el resumen de calificaciones:

```bash
curl -s -X POST "http://localhost:8080/api/nucleo/admin/moderacion/opiniones/$OPINION_ID/decision" \
  -H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" -H "X-Rol: ADMINISTRADOR" \
  -H "Content-Type: application/json" -d '{"decision": "APROBAR"}'
# para rechazar: '{"decision": "RECHAZAR", "motivo": "..."}'; el núcleo le notifica el motivo al turista

curl -s "$BASE/ofertas/$OFERTA/opiniones"
# {"resumen":{"totalOpiniones":1,"promedioCalificacion":5.0},"contenido":[...]}
```

Cuando el núcleo ejecuta la supresión de los datos de un turista (Ley 1581), el catálogo borra sus opiniones y sus habilitaciones:

```bash
docker compose -f infra/docker-compose.yml exec -T nginx \
  curl -s -X POST http://catalogo:8080/api/catalogo/internal/opiniones/avisos \
  -H "Content-Type: application/json" \
  -d "{\"idAviso\":\"supresion-$TURISTA\",\"tipo\":\"SUPRESION_TURISTA\",\"turistaId\":\"$TURISTA\"}"
# {"resultado":"APLICADO"}

curl -s "$BASE/ofertas/$OFERTA/opiniones"                 # totalOpiniones: 0
curl -s "$BASE/turista/opiniones" -H "X-Usuario-Id: $TURISTA" -H "X-Rol: TURISTA"   # totalElementos: 0
```

La precarga trae 9 opiniones aprobadas en cuatro ofertas (por ejemplo `curl -s "$BASE/ofertas/b00000000000000000000001/opiniones"`), 2 en moderación y 1 rechazada. Laura (`70000000-0000-0000-0000-000000000007`) ya pagó el snorkel en Playa Cristal y el avistamiento de aves en Minca, así que puede opinar sobre ellos sin habilitarla por consola.

### 9. Fotos de las ofertas

Las fotos son parte de la versión: cambiarlas es editar la oferta y pasa por moderación (DC-47). Con la oferta de la sección 3 ya aprobada:

```bash
# La ficha trae la galería y la búsqueda trae la portada, con la miniatura para la tarjeta
curl -s "$BASE/ofertas/$ID"
curl -s "$BASE/ofertas?zonaId=a00000000000000000000007"

# Cambiar la foto crea una versión pendiente; el turista sigue viendo la aprobada
OTRA=$(curl -s -X POST "$BASE/prestador/fotos" "${GUIA[@]}" -F "foto=@otra-foto.jpg" | sed -E 's/^\{"id":"([^"]+)".*/\1/')
curl -s -X PUT "$BASE/prestador/ofertas/$ID" "${GUIA[@]}" -H "Content-Type: application/json" -d '{
        "tipo": "ACTIVIDAD", "nombre": "Caminata nocturna en Minca",
        "descripcion": "Recorrido para observar ranas e insectos al caer la noche", "precioReferencia": 75000,
        "zonaId": "a00000000000000000000007", "capacidadMaxima": 6, "fotos": ["'"$OTRA"'", "'"$FOTO"'"],
        "tipoActividad": "AVISTAMIENTO_FAUNA", "nivelEsfuerzo": "BAJO", "horariosSalida": ["18:30"],
        "puntoEncuentro": "Plaza principal de Minca", "duracionMinutos": 120, "modalidad": "CAMINATA",
        "idiomasAtencion": ["es", "en"]}'

# La foto de otro prestador no sirve: 400 DATO_INVALIDO en "fotos". Tampoco una oferta sin fotos
AGENCIA=(-H "X-Usuario-Id: 10000000-0000-0000-0000-000000000001" -H "X-Rol: PRESTADOR" -H "X-Tipo-Prestador: AGENCIA_OPERADORA")
curl -s -X PUT "$BASE/prestador/ofertas/b00000000000000000000001" "${AGENCIA[@]}" -H "Content-Type: application/json" \
  -d '{"tipo": "ACTIVIDAD", "nombre": "Caminata", "descripcion": "Sendero", "precioReferencia": 150000,
       "zonaId": "a00000000000000000000005", "capacidadMaxima": 12, "fotos": ["'"$FOTO"'"],
       "tipoActividad": "SENDERISMO", "nivelEsfuerzo": "MODERADO", "horariosSalida": ["07:00"]}'
```

Una foto que nadie usa durante 24 horas se borra: las que se subieron y no se pusieron en una oferta, las de versiones reemplazadas y las de ofertas retiradas. La limpieza corre cada hora, en el minuto 17; para verla antes, se arranca con otra programación, por ejemplo `FOTOS_LIMPIEZA_CRON="0 * * * * *" docker compose -f infra/docker-compose.yml up -d` (cada minuto). La limpieza solo borra fotos con más de 24 horas sin uso.

### 10. Zonas del administrador

El administrador registra las zonas, las marca como área protegida o de alta afluencia, escribe sus buenas prácticas y las inactiva; una zona nunca se elimina (DC-49). Escribe directo en el catálogo, sin pasar por el núcleo.

```bash
ADMIN=(-H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" -H "X-Rol: ADMINISTRADOR")
ZONA='{"nombre": "Isla de Salamanca", "descripcion": "Parque de manglares entre Barranquilla y Santa Marta",
       "ubicacion": {"latitud": 10.98, "longitud": -74.47}'

# Registrar: responde 201 con la zona, activa. El mismo nombre con otras mayúsculas, tildes o espacios da 409 ZONA_DUPLICADA
ZONA_ID=$(curl -s -X POST "$BASE/admin/zonas" "${ADMIN[@]}" -H "Content-Type: application/json" -d "$ZONA}" \
  | sed -E 's/^\{"id":"([^"]+)".*/\1/')
curl -s -X POST "$BASE/admin/zonas" "${ADMIN[@]}" -H "Content-Type: application/json" \
  -d "${ZONA/Isla de Salamanca/ISLA DE  SALAMANCA}}"

# Marcarla como protegida con sus buenas prácticas. El PUT reemplaza todo y exige "activa"
curl -s -X PUT "$BASE/admin/zonas/$ZONA_ID" "${ADMIN[@]}" -H "Content-Type: application/json" \
  -d "$ZONA, \"areaProtegida\": true, \"buenasPracticas\": [\"No alimentar a los animales\"], \"activa\": true}"
curl -s "$BASE/zonas/$ZONA_ID"

# Inactivarla: sale de GET /zonas y no admite ofertas nuevas (400 ZONA_NO_DISPONIBLE), pero sus ofertas siguen visibles.
# Con "activa": true se reactiva
curl -s -X PUT "$BASE/admin/zonas/$ZONA_ID" "${ADMIN[@]}" -H "Content-Type: application/json" \
  -d "$ZONA, \"activa\": false}"
curl -s "$BASE/admin/zonas" "${ADMIN[@]}"
```

### 11. Recomendaciones

La recomendación es pública y va por POST: el texto del turista viaja en el cuerpo, no en la URL (DR-09). Sin `infra/.env`, responde el motor por reglas:

```bash
RECO=http://localhost:8080/api/recomendacion/recomendaciones

# Avistamiento de fauna, esfuerzo hasta moderado y 150.000 de precio maximo
curl -s -X POST "$RECO" -H "Content-Type: application/json" \
  -d '{"tiposActividad":["AVISTAMIENTO_FAUNA"],"nivelEsfuerzoMaximo":"MODERADO","precioMaximo":150000}'
# {"motor":"REGLAS","recomendaciones":[{"posicion":1,"oferta":{"id":"b0000000000000000000000a",
#   "nombre":"Avistamiento de aves en Minca",...},"criterios":["TIPO_ACTIVIDAD","NIVEL_ESFUERZO","PRECIO"],
#   "explicacion":"Coincide con: avistamiento de fauna, esfuerzo bajo, precio dentro de tu presupuesto."}, ...]}

# Las explicaciones en ingles
curl -s -X POST "$RECO" -H "Content-Type: application/json" -d '{"idioma":"en","accesibleSillaRuedas":true}'

# Menos de 3 ofertas cumplen los filtros obligatorios: devuelve las que haya, con el aviso, sin relajarlos
curl -s -X POST "$RECO" -H "Content-Type: application/json" -d '{"accesibleSillaRuedas":true,"precioMaximo":65000}'
# {"motor":"REGLAS","aviso":"POCAS_COINCIDENCIAS","recomendaciones":[...]}

# Sin texto ni filtros: 400 DATO_INVALIDO
curl -s -X POST "$RECO" -H "Content-Type: application/json" -d '{}'
```

Entre ofertas con la misma afinidad el orden cambia en cada consulta (RF-27). Cada consulta deja una línea en el registro de exposición (RET-02), sin datos del turista:

```bash
docker compose -f infra/docker-compose.yml logs recomendacion | grep exposicion
# ... exposicion : {"fecha":"...","origen":"RECOMENDACION","motor":"REGLAS","recomendaciones":[{"ofertaId":"b0000000000000000000000a",
#   "prestadorId":"20000000-0000-0000-0000-000000000002","posicion":1,"criterios":[...]}, ...]}
```

Con el catálogo apagado, la recomendación sigue respondiendo con su copia en caché. Sin copia (por ejemplo, recién arrancada) responde 503 `RECOMENDACION_NO_DISPONIBLE`, y el resto de la plataforma sigue funcionando (ADR-04):

```bash
docker compose -f infra/docker-compose.yml stop catalogo
docker compose -f infra/docker-compose.yml restart recomendacion
curl -s -X POST "$RECO" -H "Content-Type: application/json" -d '{"precioMaximo":100000}'
# 503 RECOMENDACION_NO_DISPONIBLE
docker compose -f infra/docker-compose.yml start catalogo
```

Para el motor con IA y el cambio de proveedor, ver "Motor de recomendación: reglas o IA" en "Levantar el piloto".

**Alertas del destino (RF-28).** Públicas; responden desde la caché, nunca llaman al IDEAM en la petición. Con la fuente por defecto (`ideam`), el 24-10-2026 las 7 zonas tenían alerta hidrológica amarilla:

```bash
API=http://localhost:8080/api/recomendacion

# Las de una zona (Centro Historico). Las del IDEAM no traen fecha de emision: la hora es la de la consulta
curl -s "$API/zonas/a00000000000000000000001/alertas"
# {"zonaId":"a00000000000000000000001","actualizadoEn":"2026-10-25T00:12:18.314609063Z","alertas":[
#   {"id":"ideam-hidrologica-a00000000000000000000001","tipo":"HIDROLOGICA","nivel":"AMARILLA",
#    "titulo":"Alerta hidrológica en la subzona Río Piedras - Río Manzanares","descripcion":"El IDEAM tiene ...",
#    "fuente":"IDEAM, alertas hidrológicas vigentes"}]}

# Todas, agrupadas por zona y con el nombre de la zona
curl -s "$API/alertas"
# {"actualizadoEn":"...","zonas":[{"zonaId":"a00000000000000000000001","zonaNombre":"Centro Histórico","alertas":[...]},
#   ... {"zonaId":"a00000000000000000000006","zonaNombre":"Troncal del Caribe","alertas":[{...,
#   "titulo":"Alerta hidrológica en la subzona Rio Guachaca - Mendiguaca y Buritaca",...}]}, ...]}

# En ingles: el titulo y la descripcion; la fuente no se traduce (DR-21)
curl -s "$API/zonas/a00000000000000000000001/alertas?idioma=en"
# {...,"alertas":[{...,"titulo":"Hydrological alert in the Río Piedras - Río Manzanares subzone",
#   "descripcion":"IDEAM has an active yellow hydrological alert ...","fuente":"IDEAM, alertas hidrológicas vigentes"}]}
curl -s "$API/alertas?idioma=en"
```

Con `ALERTAS_FUENTE=simulada` (ver "Fuente de las alertas" en "Levantar el piloto"):

```bash
curl -s "$API/zonas/a00000000000000000000005/alertas"     # Parque Tayrona
# {"zonaId":"a00000000000000000000005","actualizadoEn":"...","alertas":[{"id":"simulada-tayrona-cierre",
#   "tipo":"CIERRE","nivel":"NARANJA","titulo":"Cierre temporal del parque",...,
#   "fuente":"Parques Nacionales (simulado)","emitidaEn":"...","vigenteHasta":"..."}]}
curl -s "$API/zonas/a00000000000000000000007/alertas"     # Minca: DESLIZAMIENTO, AMARILLA, "IDEAM (simulado)"
curl -s "$API/zonas/a00000000000000000000001/alertas"     # Centro Historico: su alerta ya vencio, no se muestra
# {"zonaId":"a00000000000000000000001","actualizadoEn":"...","alertas":[]}
curl -s "$API/alertas"                                    # Taganga, Parque Tayrona y Minca
curl -s "$API/zonas/a00000000000000000000005/alertas?idioma=en"   # "Temporary park closure"
```

**Alternativas por alta afluencia (RF-29).** Para la ficha de una oferta: si su zona tiene alta afluencia (Parque Tayrona, Taganga), de 2 a 3 ofertas del mismo tipo en zonas con menos visitantes, por regla y sin IA (DR-18, DR-19). Entre empatadas el orden rota, así que la segunda y la tercera cambian entre consultas:

```bash
# Caminata a Cabo San Juan, en el Parque Tayrona
curl -s "$API/ofertas/b00000000000000000000001/alternativas"
# {"aplica":true,"zonaOrigen":{"id":"a00000000000000000000005","nombre":"Parque Tayrona"},"alternativas":[
#   {"posicion":1,"oferta":{"id":"b0000000000000000000000c","nombre":"Caminata a Pozo Azul y cascada de Marinka",...},
#    "criterios":["TIPO_OFERTA","TIPO_ACTIVIDAD","NIVEL_ESFUERZO","PRECIO"],
#    "explicacion":"Mismo tipo de oferta en Minca y Sierra Nevada, una zona con menos visitantes; también de
#    senderismo, con esfuerzo igual o menor y precio parecido."}, {"posicion":2,...}, {"posicion":3,...}]}

# En ingles
curl -s "$API/ofertas/b00000000000000000000001/alternativas?idioma=en"

# Paquete de Taganga: solo un paquete cumple, con el aviso
curl -s "$API/ofertas/b00000000000000000000009/alternativas"
# {"aplica":true,"aviso":"POCAS_COINCIDENCIAS","zonaOrigen":{...,"nombre":"Taganga"},"alternativas":[{...}]}

# Recorrido por el Centro Historico: la zona no tiene alta afluencia, la ficha no muestra nada
curl -s "$API/ofertas/b00000000000000000000006/alternativas"
# {"aplica":false,"alternativas":[]}

# Una oferta que no existe o no es visible: 404 OFERTA_NO_ENCONTRADA
curl -s "$API/ofertas/b000000000000000000000ff/alternativas"
```

Las alternativas también dejan su línea en el registro de exposición, con `"origen":"ALTERNATIVAS"` (las recomendaciones llevan `"origen":"RECOMENDACION"`), para que la métrica de sesgo las separe (DR-20).

### 12. Rutas que no se exponen

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/internal/avisos"      # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/internal/ofertas"             # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/internal/moderacion/ofertas/b00000000000000000000017"  # 404
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/internal/opiniones/avisos"  # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/actuator"                     # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/actuator/health"              # 404
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/actuator/prometheus"          # 404: las métricas solo dentro de Docker
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/recomendacion/actuator/health  # 404
```

Otros casos que se pueden mostrar:

- **Sin identidad:** el mismo `POST` sin los headers responde 401 `NO_AUTENTICADO`.
- **Rol equivocado:** el mismo `POST` con `X-Rol: TURISTA` responde 403 `ACCESO_DENEGADO`; un prestador que pide `/turista/opiniones` recibe lo mismo.
- **RNT inactivo:** con `X-Usuario-Id: 50000000-0000-0000-0000-000000000005`, `X-Rol: PRESTADOR` y `X-Tipo-Prestador: AGENCIA_OPERADORA` responde 403 `RNT_INACTIVO`.
- **Tipo no permitido:** el guía que publica un `ALOJAMIENTO` recibe 403 `TIPO_OFERTA_NO_PERMITIDO`.
- **Edición:** antes de retirarla, un `PUT $BASE/prestador/ofertas/$ID` con otro precio crea la versión 2 en moderación; la ficha sigue mostrando la versión aprobada hasta que llegue su aviso.
- **Oferta retirada:** después del retiro, ese mismo `PUT` responde 409 `OFERTA_NO_EDITABLE`.

## Cómo contribuir

El flujo de ramas (GitFlow), los pull requests y la guía de mensajes de commit están en [CONTRIBUTING.md](CONTRIBUTING.md).
