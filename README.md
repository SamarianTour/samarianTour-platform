# Samarian Tour

Plataforma de gestión sostenible del turismo en Santa Marta. Monorepo Maven multimódulo con arquitectura basada en servicios (`nucleo`, `catalogo`, `recomendacion`) y arquitectura hexagonal dentro de cada módulo de cada servicio (ADR-02).

> En construcción. El prototipo es el servicio de catálogo: búsqueda y ficha de ofertas, zonas, publicación y edición por el prestador, y moderación por avisos del núcleo. Todavía guarda los datos en memoria; el adaptador de MongoDB, el núcleo y la recomendación están pendientes.

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `servicios/catalogo` | Servicio de catálogo (MongoDB). Módulos `catalogo` y `opiniones`, más `compartido`. |
| `servicios/nucleo` | Núcleo transaccional (PostgreSQL). Esqueleto. |
| `servicios/recomendacion` | Recomendación (sin base de datos propia). Esqueleto. |
| `libs/seguridad-jwt` | Validación del JWT RS256 y roles, compartida por los tres servicios. Pendiente. |
| `contratos/` | Contratos OpenAPI (API first). Está el del catálogo: `catalogo.openapi.yaml`. |
| `frontend/` | Angular. Fuera del alcance del prototipo. |
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
│       ├── salida/memoria/     adaptadores en memoria del prototipo (perfil memoria)
│       └── salida/persistencia/ documentos Mongo, repositorios y mappers
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

Java 21, Maven 3.9 y Docker (las pruebas de integración usan Testcontainers).

## Compilar y probar

```bash
mvn test      # unitarias, controladores, contrato y ArchUnit (no necesita Docker)
mvn verify    # además, las de integración (*IT) con MongoDB en Testcontainers
```

`ContratoCatalogoTest` compara las rutas de los controladores con las de `contratos/catalogo.openapi.yaml`: si difieren, el build falla.

## Perfiles del catálogo

| Perfil | Qué activa | Cuándo |
|---|---|---|
| `memoria` | Adaptadores en la memoria del proceso, con datos de demostración de Santa Marta | Por defecto, mientras no exista el adaptador de MongoDB |
| `demo` | Los headers que sustituyen al token del prestador | Nunca por defecto; solo en la demostración |

**Identidad provisional del prestador.** Mientras no exista `libs/seguridad-jwt`, las rutas `/prestador/**` identifican al prestador con dos headers: `X-Prestador-Id` (UUID) y `X-Tipo-Prestador` (`AGENCIA_OPERADORA`, `GUIA_INDEPENDIENTE`, `ALOJAMIENTO` o `RESTAURANTE`). Cualquiera puede enviarlos, así que solo se aceptan con el perfil `demo`; sin él responden 401. Los reemplaza el JWT que emitirá el núcleo, y entonces solo cambia `compartido/seguridad`.

## Levantar el piloto

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

Nginx queda en `http://localhost:8080` y enruta `/api/catalogo/**` hacia las 2 réplicas del catálogo. Cada respuesta trae el header `X-Instancia` con el hostname del contenedor que la atendió, y los errores salen como `application/problem+json` (RFC 7807). Las rutas `/api/catalogo/internal/**` y `/api/catalogo/actuator/**` no se exponen hacia afuera.

Compose activa los perfiles `memoria` y `demo`. Nginx arranca cuando el catálogo responde su healthcheck (`/actuator/health`).

Actualizar solo el catálogo sin detener lo demás:

```bash
docker compose -f infra/docker-compose.yml up -d --build --no-deps catalogo
```

## Demostración paso a paso

Los comandos están en `bash` (en Windows, Git Bash). Los datos de demostración son inventados: 7 zonas, 5 prestadores y 23 ofertas de los cuatro tipos.

```bash
BASE=http://localhost:8080/api/catalogo
```

### 1. Búsqueda con filtros, repartida entre las réplicas

```bash
# El header X-Instancia muestra qué réplica respondió: Nginx reparte las peticiones entre las dos, sin un orden fijo
for i in 1 2 3 4 5 6 7 8; do curl -s -o /dev/null -D - "$BASE/ofertas" | grep -i '^x-instancia'; done

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
# Paquete: sus actividades, el nivel de esfuerzo calculado, la zona protegida y el perfil del prestador
curl -s "$BASE/ofertas/b00000000000000000000008"

# Oferta aprobada de un prestador con el RNT inactivo: no es visible, responde 404
curl -s "$BASE/ofertas/b00000000000000000000016"

curl -s "$BASE/zonas"
```

### 3. Publicar, aprobar y ver la oferta en la búsqueda

Con los datos en memoria, cada réplica tiene su propio estado: lo que se publica en una no lo ve la otra. Para este flujo se deja una sola réplica; con el adaptador de MongoDB el estado será compartido y no hará falta.

```bash
docker compose -f infra/docker-compose.yml up -d --scale catalogo=1 --no-recreate
```

Publicar una actividad como el guía independiente de la demostración:

```bash
ID=$(curl -s -X POST "$BASE/prestador/ofertas" \
  -H "Content-Type: application/json" \
  -H "X-Prestador-Id: 20000000-0000-0000-0000-000000000002" \
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
  -d "{\"idAviso\":\"demo-1\",\"tipo\":\"MODERACION\",\"ofertaId\":\"$ID\",\"numeroVersion\":1,\"decision\":\"APROBAR\"}"
# {"resultado":"APLICADO"}; si se repite el mismo aviso, {"resultado":"DUPLICADO"}
```

Ahora aparece en la ficha y en la búsqueda:

```bash
curl -s "$BASE/ofertas/$ID"
curl -s "$BASE/ofertas?zonaId=a00000000000000000000007&tipoActividad=AVISTAMIENTO_FAUNA"
```

Para volver a las dos réplicas:

```bash
docker compose -f infra/docker-compose.yml up -d --scale catalogo=2 --no-recreate
```

Otros casos que se pueden mostrar:

- **Sin identidad:** el mismo `POST` sin los headers responde 401 `NO_AUTENTICADO`.
- **RNT inactivo:** con `X-Prestador-Id: 50000000-0000-0000-0000-000000000005` y `X-Tipo-Prestador: AGENCIA_OPERADORA` responde 403 `RNT_INACTIVO`.
- **Tipo no permitido:** el guía que publica un `ALOJAMIENTO` recibe 403 `TIPO_OFERTA_NO_PERMITIDO`.
- **Edición:** un `PUT $BASE/prestador/ofertas/$ID` con otro precio crea la versión 2 en moderación; la ficha sigue mostrando la versión aprobada hasta que llegue su aviso.
- **Ofertas propias:** `GET $BASE/prestador/ofertas` con los mismos headers lista las ofertas del guía en cualquier estado, con su versión publicada y la pendiente.
- **Retiro:** `curl -s -o /dev/null -w '%{http_code}\n' -X PATCH "$BASE/prestador/ofertas/$ID/retiro"` con los mismos headers responde 204. La oferta sale de la búsqueda, sigue en la lista de ofertas propias como `"retirada": true` y un `PUT` posterior responde 409 `OFERTA_NO_EDITABLE`.

## Cómo contribuir

El flujo de ramas (GitFlow), los pull requests y la guía de mensajes de commit están en [CONTRIBUTING.md](CONTRIBUTING.md).
