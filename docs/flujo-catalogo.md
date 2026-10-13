# Cómo funciona el catálogo: flujo explicado

> Marcado con 🔨 lo que todavía no está construido.

## 1. ¿Qué es el sistema?

Samarian Tour es una plataforma de turismo para Santa Marta: los **prestadores** (agencias, guías, hoteles, restaurantes) publican sus servicios, a los que llamamos **ofertas**; los **turistas** las buscan y envían solicitudes; un **administrador** revisa (**modera**) lo que se publica antes de que el público lo vea.

## 2. Las piezas grandes

Son **tres servicios** independientes, cada uno en su propio contenedor Docker, detrás de un **Nginx** que reparte el tráfico según la ruta.

```
                    Navegador (turista, prestador, administrador)
                                  │
                                  ▼
                 ┌─────────────────────────────────┐
                 │  Nginx (única puerta de entrada)│
                 │  /api/catalogo/**  → catálogo   │
                 │  /api/nucleo/**    → núcleo     │
                 │  /api/recomendacion/** → recom. │
                 └─────────────────────────────────┘
                     │                │                │
                     ▼                ▼                ▼
               ┌──────────┐    ┌───────────┐    ┌──────────────┐
               │ CATÁLOGO │◄───│  NÚCLEO   │    │ RECOMENDACIÓN│
               │ réplica 1│avisos (cuentas,│    │ (sin base de │
               │ réplica 2│    │ pagos,     │    │   datos)     │
               └────┬─────┘    │ prestadores│    └──────────────┘
                    │          │ moderación)│
                    ▼          └─────┬──────┘
                MongoDB              ▼
          (ofertas, zonas)       PostgreSQL
```

| Servicio | De qué se encarga | Base de datos | Estado |
|---|---|---|---|
| **Núcleo** | Cuentas, login (emite el token JWT), datos de los prestadores y su RNT, solicitudes, pagos y la decisión del administrador al moderar | PostgreSQL | Esqueleto 🔨 |
| **Catálogo** | Zonas, ofertas, perfiles públicos y opiniones | MongoDB | En construcción |
| **Recomendación** | Sugerencias y alertas del clima | Ninguna | Esqueleto 🔨 |

**¿Por qué separarlos?** En temporada alta casi todo el tráfico es gente mirando el catálogo. Al separarlo, se levantan varias copias del catálogo (2 réplicas) sin tocar el núcleo, donde está el dinero (ADR-02).

**¿Cómo se hablan?** Cada servicio es dueño de sus datos y nadie lee la base de otro. El núcleo le cuenta cosas al catálogo ("el administrador aprobó esta oferta", "este prestador perdió el RNT") con **avisos** HTTP a una ruta interna (`/internal/avisos`) que Nginx no expone hacia afuera.

## 3. Cómo está organizado un servicio por dentro (hexagonal)

Dentro del catálogo hay **módulos** (`catalogo`, `opiniones`, más `compartido` para lo técnico común). Cada módulo tiene tres capas:

```
  INFRAESTRUCTURA  (HTTP, JSON, MongoDB, Spring)
     entrada/rest: controladores, DTO, mappers
     salida/persistencia: documentos Mongo, repositorios, mappers
        │                          ▲
        ▼                          │
     APLICACIÓN  (un servicio por caso de uso: "busca esto, llama al dominio, guarda")
        │
        ▼
     DOMINIO  (Java puro: sin Spring ni Mongo)
        modelo: Oferta, VersionOferta…
        politica: reglas de negocio
        puerto/entrada: qué se puede pedir (casos de uso)
        puerto/salida: qué necesita de afuera (guardar, buscar…)
```

- Las dependencias apuntan **hacia adentro**: el dominio no sabe que existen HTTP, MongoDB ni Spring.
- **Puertos:** interfaces que define el dominio (por ejemplo `OfertaRepositoryPort`: "necesito guardar ofertas").
- **Adaptadores:** implementan los puertos con una tecnología (`OfertaMongoAdapter` guarda en MongoDB). Cambiar MongoDB por PostgreSQL solo exige reescribir el adaptador (ADR-03).
- **Mappers:** el web traduce JSON (DTO) ↔ dominio; el de persistencia, dominio ↔ documento Mongo.
- **Servicios de aplicación:** cada puerto de entrada tiene su propia implementación (DC-21): `GestionarOfertaService`, `BuscarOfertasService`, `ConsultarFichaOfertaService`, `ConsultarZonasService` y `AplicarAvisoNucleoService`.
- **ArchUnit** (`ArquitecturaTest`) verifica 8 reglas en cada build; si alguien las rompe, el build falla.

## 4. Conceptos del negocio

### Oferta y versión

- **Oferta:** la *identidad* (id fijo, tipo que nunca cambia, dueño, si está retirada). Solicitudes y opiniones apuntan aquí.
- **Versión:** el *contenido* (nombre, descripción, precio, horarios…). Cada edición crea una versión nueva.

```
Oferta "Caminata a Cabo San Juan"
 ├── publicada:  versión 1  (APROBADA)       ← la que ve el turista
 └── pendiente:  versión 2  (EN_MODERACION)  ← la edición que espera revisión
```

Mientras una edición espera moderación, el turista sigue viendo la última aprobada (RF-66).

- Estados de una versión: **EN_MODERACION** → **APROBADA** (pasa a publicada) o **RECHAZADA** (queda como pendiente con su motivo).
- El **número de versión** siempre crece y nunca se repite: "aprobar la versión 2" aprueba exactamente la 2.

### RNT y habilitación

- El **RNT** es el permiso legal del prestador. Sin RNT activo no puede publicar y sus ofertas no se muestran.
- El dato real vive en el núcleo. El catálogo guarda una **réplica** (`PrestadorCatalogo`) que se actualiza con avisos.
- Cada oferta guarda además una **copia** (`rntPrestadorActivo`) para que la búsqueda sea rápida (QAS-07).

### ¿Cuándo ve el turista una oferta? (`ReglaVisibilidad`)

1. Tiene versión **publicada**.
2. **No** está retirada.
3. El **RNT** del prestador está activo.

### ¿Quién hace la petición?

En la versión final, el token JWT del núcleo trae id, rol y tipo de prestador. En el prototipo el id y el tipo llegan en headers (`X-Prestador-Id` y `X-Tipo-Prestador`) que simulan el token y solo se aceptan con el perfil `demo`; los avisos del núcleo se simulan con `curl`.

## 5. Flujo completo, paso a paso

Personajes: **Laura**, guía independiente de Minca, y **Pedro**, turista.

### Paso 0: el núcleo habilita a Laura

```
POST /api/catalogo/internal/avisos
{ idAviso: "a-1", tipo: HABILITACION, prestadorId: Laura, rntActivo: true, version: 1 }
```

1. `AvisoInternoController` recibe el aviso y llama a `AplicarAvisoNucleoUseCase`, que implementa `AplicarAvisoNucleoService`.
2. ¿El aviso `a-1` ya fue procesado? No.
3. Se busca la réplica de Laura; si no existe, se trata como **no habilitada**.
4. `aplicarHabilitacion(true, 1)`: la versión 1 es mayor que 0, así que `rntActivo = true`.
5. Se actualiza la copia en todas las ofertas de Laura.
6. Se marca `a-1` como procesado.

Un aviso repetido se detecta en el paso 2; un aviso viejo lo descarta `aplicarHabilitacion` (idempotencia, ADR-04).

### Paso 1: Laura publica una actividad

```
POST /api/catalogo/prestador/ofertas
Headers: X-Prestador-Id: Laura, X-Tipo-Prestador: GUIA_INDEPENDIENTE
Cuerpo: { tipo: "ACTIVIDAD", nombre: "Avistamiento de aves", zonaId: "minca", precio: 80000, ... }
```

```
Nginx ──► réplica 1 o 2 del catálogo
   ├─ InstanciaHeaderFilter: agrega X-Instancia (qué réplica atendió)
   ▼
OfertaPrestadorController
   ├─ ResolutorIdentidad: toma el prestador de los headers (solo con el perfil demo; si no, 401)
   └─ el mapper web convierte el JSON en objetos del dominio
   ▼
GestionarOfertaService.publicar
   1. Busca la réplica de Laura     (puerto de salida → MongoDB)
   2. Busca la zona "minca"         (si no existe: ZONA_NO_DISPONIBLE)
   3. Llama al dominio:
   ▼
Oferta.publicar  (aquí viven las reglas)
   ¿RNT activo?                         no → 403 RNT_INACTIVO
   ¿Un guía puede publicar ACTIVIDAD?   no → 403 TIPO_OFERTA_NO_PERMITIDO
   ¿Es guía? Exige punto de encuentro,
     duración, modalidad e idiomas      falta → 400 DATO_INVALIDO (RF-67)
   ¿Zona activa?                        no → 400 ZONA_NO_DISPONIBLE
   OK → pendiente = versión 1 (EN_MODERACION), publicada = vacía
   ▼
GestionarOfertaService guarda → OfertaMemoriaAdapter (hoy) u OfertaMongoAdapter 🔨 → documento en MongoDB
   ▼
201 Created
```

Si algo falla, el manejador de errores (`ManejadorErrores`) responde en formato ProblemDetail con un `codigo`.

### Paso 2: Pedro busca y no la ve

`GET /api/catalogo/ofertas?tipo=ACTIVIDAD&zonaId=minca`: `BuscarOfertasService` solo trae ofertas visibles, y la de Laura no tiene versión publicada.

### Paso 3: el administrador la aprueba

```
POST /api/catalogo/internal/avisos
{ idAviso: "a-2", tipo: MODERACION, ofertaId: ..., numeroVersion: 1, decision: "APROBAR" }
```

1. ¿`a-2` ya fue procesado? No.
2. `Oferta.aprobar(1, rntActivo de la réplica, ahora)`: la versión 1 pasa a **publicada** y se vuelve a copiar el RNT.
3. Se guarda la oferta y se marca `a-2` como procesado.

Si Laura ya hubiera enviado la versión 2, el aviso de la versión 1 se ignora.

### Paso 4: Pedro busca y ahora sí la ve

La búsqueda usa el **índice parcial** de MongoDB (QAS-07), que solo contiene ofertas visibles ordenadas por tipo, zona y precio.

### Paso 5: Laura edita el precio (regla de la sustentación)

`Oferta.editar` verifica dueño, que no esté retirada, RNT activo, tipo y zona, y crea la **versión 2**:

```
publicada: versión 1  (80.000, APROBADA)       ← Pedro sigue viendo esta
pendiente: versión 2  (90.000, EN_MODERACION)
```

Al aprobarse, la 2 pasa a publicada. Si se rechaza, la 2 queda pendiente con motivo y Pedro sigue viendo la 1.

### Paso 6: a Laura le suspenden el RNT

Aviso `{ a-3, HABILITACION, Laura, rntActivo: false, version: 2 }`:

1. La réplica queda con `rntActivo = false`.
2. Un `updateMany` pone `rntPrestadorActivo = false` en todas sus ofertas.
3. Las ofertas salen del índice parcial y Pedro ya no las ve.
4. Si Laura intenta publicar o editar: 403 RNT_INACTIVO. Retirar sí puede.

Con un aviso posterior (`version: 3`, `true`) todo vuelve a ser visible.

### Paso 7: Laura retira la oferta

`PATCH /api/catalogo/prestador/ofertas/{id}/retiro` → 204 (el caso de uso existe; el endpoint todavía no 🔨). Se marca `retirada = true`, no se borra (las solicitudes en curso siguen apuntando a ella). Es definitivo.

## 6. Dónde está cada cosa

Base: `servicios/catalogo/src/main/java/co/edu/unimagdalena/samariantour/catalogo/`

| Concepto | Archivo |
|---|---|
| Reglas de la oferta | `catalogo/dominio/modelo/Oferta.java` |
| Contenido y tipos | `VersionOferta`, `VersionActividad`, `VersionAlojamiento` |
| Réplica RNT | `PrestadorCatalogo.java` |
| Qué puede publicar cada prestador | `dominio/politica/ReglaTipoOferta.java` |
| Cuándo es visible | `dominio/politica/ReglaVisibilidad.java` |
| Validaciones de campo | `dominio/modelo/Validaciones.java` |
| Errores con código | `dominio/excepcion/*` |
| Casos de uso | `dominio/puerto/entrada/` |
| Lo que el dominio necesita de afuera | `dominio/puerto/salida/` |
| Coordinación de pasos | `aplicacion/`: `GestionarOfertaService`, `BuscarOfertasService`, `ConsultarFichaOfertaService`, `ConsultarZonasService`, `AplicarAvisoNucleoService` |
| Controladores y JSON | `infraestructura/entrada/rest/` |
| Creación de los servicios (Spring) | `infraestructura/config/CatalogoConfig` |
| Datos en memoria del prototipo | `infraestructura/salida/memoria/` |
| MongoDB | `infraestructura/salida/persistencia/` 🔨 |
| Header X-Instancia | `compartido/config/InstanciaHeaderFilter` |
| Simulación del token | `compartido/seguridad/` |
| Reglas de arquitectura | `src/test/.../ArquitecturaTest.java` |

Fuera del código: `infra/docker-compose.yml`, `infra/nginx/nginx.conf`, `infra/mongo/init/` (precarga de zonas 🔨), `.github/workflows/ci.yml`.

## 7. Resumen por capa

- **Dominio:** *qué* se puede hacer y bajo qué reglas.
- **Aplicación:** *en qué orden* se hacen los pasos.
- **Infraestructura:** *con qué tecnología*.
- **Nginx:** *a qué servicio y réplica* va cada petición.
- **Avisos:** cómo el núcleo le cuenta al catálogo lo que decidió, sin compartir bases de datos.
