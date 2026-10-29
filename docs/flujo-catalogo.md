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
     (ofertas, zonas, opiniones) PostgreSQL
```

| Servicio | De qué se encarga | Base de datos | Estado |
|---|---|---|---|
| **Núcleo** | Cuentas, login (emite el token JWT), datos de los prestadores y su RNT, solicitudes, pagos y la decisión del administrador al moderar | PostgreSQL | Moderación, solicitudes de disponibilidad y pagos; lo demás 🔨 |
| **Catálogo** | Zonas, ofertas, perfiles públicos y opiniones | MongoDB | En construcción |
| **Recomendación** | Recomendaciones explicadas, por reglas o con IA; alertas del destino (IDEAM) y alternativas en zonas con menos visitantes | Ninguna | Prototipo |

**¿Por qué separarlos?** En temporada alta casi todo el tráfico es gente mirando el catálogo. Al separarlo, se levantan varias copias del catálogo (2 réplicas) sin tocar el núcleo, donde está el dinero (ADR-02).

**¿Cómo se hablan?** Cada servicio es dueño de sus datos y nadie lee la base de otro. El núcleo le cuenta cosas al catálogo ("el administrador aprobó esta oferta", "este prestador perdió el RNT") con **avisos** HTTP a rutas internas (`/internal/avisos` para las ofertas y `/internal/opiniones/avisos` para las opiniones) que Nginx no expone hacia afuera.

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

En la versión final, el token JWT del núcleo trae id, rol y tipo de prestador. En el prototipo llegan en headers (`X-Usuario-Id`, `X-Rol` y `X-Tipo-Prestador`) que simulan el token y solo se aceptan con el perfil `demo`; los avisos del núcleo se simulan con `curl`.

## 5. Flujo completo, paso a paso

Personajes: **Laura**, guía independiente de Minca, y **Pedro**, turista. Los pasos 8 a 11 son del módulo de opiniones.

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
Headers: X-Usuario-Id: Laura, X-Rol: PRESTADOR, X-Tipo-Prestador: GUIA_INDEPENDIENTE
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
GestionarOfertaService guarda → OfertaMongoAdapter (perfil mongo: documento en MongoDB) u OfertaMemoriaAdapter (perfil memoria)
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

`PATCH /api/catalogo/prestador/ofertas/{id}/retiro` → 204, sin cuerpo. Se marca `retirada = true`, no se borra (las solicitudes en curso siguen apuntando a ella). Es definitivo: la oferta sale de la búsqueda, ya no se puede editar (409 OFERTA_NO_EDITABLE) y sigue apareciendo como retirada en `GET /api/catalogo/prestador/ofertas`, la lista de ofertas propias.

### Paso 8: Pedro paga y queda habilitado para opinar

Pedro reservó y pagó el avistamiento de aves en el núcleo. El catálogo no le pregunta al núcleo quién pagó: el núcleo se lo avisa.

```
POST /api/catalogo/internal/opiniones/avisos
{ idAviso: "o-1", tipo: HABILITACION_OPINION, turistaId: Pedro, ofertaId: ... }
```

`AvisoOpinionesController` llama a `AplicarAvisoOpinionUseCase`, que implementa `AplicarAvisoOpinionService`: si `o-1` no fue procesado y la oferta existe, guarda la **habilitación** (Pedro, oferta) y marca el aviso.

### Paso 9: Pedro opina

```
POST /api/catalogo/turista/opiniones
Headers: X-Usuario-Id: Pedro, X-Rol: TURISTA
Cuerpo: { ofertaId: ..., calificacion: 5, comentario: "Vimos tucanes desde el primer mirador." }
```

```
OpinionTuristaController
   ▼
PublicarOpinionService
   1. Valida calificación (1 a 5) y comentario (10 a 1000)   no → 400 DATO_INVALIDO
   2. ¿La oferta existe?  (OfertaCatalogoPort → catalogo.api)  no → 404 OFERTA_NO_ENCONTRADA
   3. ¿Pedro está habilitado?                                  no → 403 OPINION_NO_HABILITADA
   4. ¿Ya opinó sobre esta oferta? (RF-61)                     sí → 409 OPINION_DUPLICADA
   ▼
Opinion.enviar → estado EN_MODERACION
   ▼
201 Created; en la ficha todavía no aparece
```

El módulo `opiniones` no conoce las clases del catálogo: le pregunta por la oferta a la interfaz `catalogo.api.ConsultaOfertaApi` (DC-40).

### Paso 10: el administrador la aprueba

1. El administrador ve la opinión en `GET /api/catalogo/admin/moderacion/opiniones`, con el nombre de la oferta.
2. El núcleo consulta `GET /api/catalogo/internal/moderacion/opiniones/{id}` para saber a qué turista notificar.
3. La decisión llega como aviso `{ o-2, MODERACION_OPINION, opinionId, decision: "APROBAR" }`. `Opinion.aprobar` la pasa a **APROBADA**; si ya no estaba en moderación, el aviso se ignora.

Ahora `GET /api/catalogo/ofertas/{id}/opiniones` la muestra, con el resumen: total de opiniones aprobadas y promedio con un decimal. Pedro ve su opinión y su estado en `GET /api/catalogo/turista/opiniones`; si la hubieran rechazado, vería el motivo y no podría enviar otra.

### Paso 11: Pedro pide suprimir sus datos

Por la Ley 1581, Pedro puede pedir al núcleo que borre sus datos. El núcleo avisa `{ o-3, SUPRESION_TURISTA, turistaId: Pedro }` y el catálogo **borra** sus opiniones y sus habilitaciones. El resumen de la oferta se recalcula solo, porque se calcula al leer.

### Paso 12: Pedro pide recomendaciones

Pedro no sabe qué buscar y le pide ideas al servicio de **recomendación**, que no tiene base de datos: su conocimiento son las ofertas visibles del catálogo (RF-26).

```
POST /api/recomendacion/recomendaciones
Cuerpo: { texto: "quiero ver aves sin caminar mucho", tiposActividad: ["AVISTAMIENTO_FAUNA"], nivelEsfuerzoMaximo: "MODERADO" }
```

```
RecomendacionController
   ▼
RecomendacionService
   1. Ofertas visibles del catálogo        CatalogoRestClientAdapter → GET /api/catalogo/internal/ofertas
                                           (caché de 60 s; si el catálogo cae y no hay copia → 503)
   2. Filtros duros y afinidad             ReglaCriteriosExplicitos: accesibilidad, precio y esfuerzo
                                           nunca se relajan; tipo, actividad y zona suman puntos
   3. Rotación entre empates               PoliticaRotacion: ningún prestador queda siempre primero
   4. El motor elige 3 a 5 y las explica   IA (si está configurada) o reglas
        └─ si la IA falla o su respuesta no pasa ValidacionRespuestaIA → responde el motor por reglas
   5. Registro de exposición               qué ofertas y prestadores se mostraron, sin datos de Pedro
   ▼
200 { motor: "REGLAS", recomendaciones: [ { posicion: 1, oferta: {...}, criterios: [...], explicacion: "..." } ] }
```

La respuesta dice qué motor eligió (`motor`) y, en cada recomendación, por qué (`criterios` y `explicacion`). Nunca por calificaciones, comisión ni por quién presta el servicio. Las decisiones están en [decisiones-recomendacion.md](decisiones-recomendacion.md).

### Paso 13: Pedro mira una oferta del Tayrona: alertas y alternativas

Pedro abre la ficha de la caminata a Cabo San Juan, en el Parque Tayrona. Además de pedirle la ficha al catálogo, la pantalla le pide dos cosas a la recomendación:

```
GET /api/recomendacion/zonas/{tayrona}/alertas?idioma=es
   AlertaController → ConsultarAlertasService → solo la caché (AlertaCacheCaffeineAdapter)
   200 { zonaId, actualizadoEn, alertas: [ { tipo: "HIDROLOGICA", nivel: "AMARILLA", titulo, fuente: "IDEAM, ..." } ] }
   (503 ALERTAS_NO_DISPONIBLES si la fuente nunca ha respondido: la pantalla omite la sección)

GET /api/recomendacion/ofertas/{caminata}/alternativas?idioma=es
   AlternativaController → ProponerAlternativasService
   1. La oferta, entre las visibles del catálogo     si no está → 404
   2. ¿Su zona es de alta afluencia?                  el Tayrona sí; si no → { aplica: false, alternativas: [] }
   3. ReglaAlternativas                               mismo tipo, zonas sin alta afluencia, afinidad y rotación
   4. Registro de exposición con origen ALTERNATIVAS
   200 { aplica: true, zonaOrigen: { id, nombre: "Parque Tayrona" }, alternativas: [ { posicion: 1, oferta: {...}, ... } ] }
```

Las alertas no se consultan al IDEAM en la petición de Pedro: `AlertasScheduler` lee la fuente al arrancar y cada 8 horas (y cada 2 minutos mientras no tenga ninguna lectura) y deja la lectura en caché, con los textos en español e inglés. Con la fuente `ideam` se consulta la capa de alertas hidrológicas vigentes con las coordenadas de cada zona; con `simulada`, un archivo de la demo cuyas fuentes dicen "(simulado)". Las alternativas salen de una regla, sin IA, con una explicación como "Mismo tipo de oferta en Minca y Sierra Nevada, una zona con menos visitantes; también de senderismo, con esfuerzo igual o menor y precio parecido." (DR-14 a DR-20).

### Paso 14: Pedro pide disponibilidad y el operador la atiende

Antes de pagar (paso 8), Pedro pide disponibilidad al **núcleo**, que es dueño de las solicitudes (DN-09 a DN-17). El catálogo solo responde la consulta de la oferta.

```
POST /api/nucleo/turista/solicitudes          Idempotency-Key: <uuid>
Cuerpo: { ofertaId, fechaInicio (≥ 2 días), personas, nota } y los campos de su tipo:
        horaSalida (actividad y paquete), fechaSalida y numeroUnidades (alojamiento) u horaReserva (restaurante)
   ▼
EnviarSolicitudService, en una transacción con la clave de idempotencia
   1. WhatsApp verificado del turista              ContactoPort (tabla de la demo)
   2. La oferta visible                             GET /api/catalogo/internal/ofertas/{id}, corte de circuito de 3 s
   3. El operador del prestador (RF-32)             PrestadoresPort (tabla de la demo)
   4. Fecha, campos, personas y no duplicado        SolicitudDisponibilidad, ReglaNoDuplicado e índice único
   5. Pendiente, 24 h para el operador              más su aviso al operador y la notificación a Pedro
   ▼
201 { id, estado: "PENDIENTE", venceEn, historial }   (con la misma clave, la misma respuesta)
```

El operador ve su cola (`GET /api/nucleo/operador/solicitudes`), verifica con el prestador fuera de la plataforma y la marca **Atendida** con el precio base, la política de cancelación y el aparte acordado, o **No disponible**. Desde Atendida corre la ventana de 3 h, o hasta las 08:00 si fue de noche, para que Pedro **confirme** o **decline** (`POST /api/nucleo/turista/solicitudes/{id}/respuesta`) y pague. Una tarea programada vence las que no se atendieron en 24 h (Sin respuesta) y las que no se pagaron a tiempo (Vencida), y cada cambio deja su notificación por WhatsApp, simulada en el log por ahora.

### Paso 15: Pedro paga y el núcleo avisa al catálogo

El pago es del **núcleo** (DN-18 a DN-27); el catálogo solo recibe el aviso del paso 8.

```
POST /api/nucleo/turista/solicitudes/{id}/pago      Idempotency-Key: <uuid>
   ▼
201 { intentoId, numero, montoCentavos, checkout: { pasarela, url } }   la página de la pasarela simulada o el Web Checkout de Wompi
   ▼
Pedro paga en la pasarela; la pasarela envía el evento firmado
POST /api/nucleo/pagos/eventos   → 200 de inmediato; una tarea lo procesa en los 5 s siguientes
   ▼
En una transacción: el intento Aprobado, la solicitud Pagada, la dispersión pendiente al prestador
y el aviso HABILITACION_OPINION en la bandeja  →  POST /api/catalogo/internal/opiniones/avisos (paso 8)
```

Si la pasarela rechaza el pago, la solicitud queda con el pago fallido y Pedro puede intentar de nuevo, hasta tres veces. Si el pago llega después de la ventana, la solicitud no se paga: queda como pago tardío para que el administrador lo reembolse.

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
| Carga de los datos de demostración en MongoDB | `infraestructura/salida/persistencia/CargaDemoMongo` |
| MongoDB | `infraestructura/salida/persistencia/` (adaptadores), `document/` y `mapper/` |
| Datos de demostración | `infraestructura/salida/demo/` |
| Lo que otros módulos pueden pedirle al catálogo | `catalogo/api/ConsultaOfertaApi`, implementada en `catalogo/infraestructura/entrada/modulos/` |
| Opiniones: reglas y estados | `opiniones/dominio/modelo/Opinion.java`, `HabilitacionOpinion`, `ResumenCalificaciones` |
| Opiniones: casos de uso | `opiniones/aplicacion/`: `PublicarOpinionService`, `LeerOpinionesOfertaService`, `ListarOpinionesTuristaService`, `ListarOpinionesPendientesService`, `AplicarAvisoOpinionService` |
| Opiniones: controladores, MongoDB y datos de demostración | `opiniones/infraestructura/entrada/rest/`, `opiniones/infraestructura/salida/persistencia/`, `opiniones/infraestructura/salida/demo/DatosOpiniones` |
| Header X-Instancia | `compartido/config/InstanciaHeaderFilter` |
| Simulación del token | `compartido/seguridad/` |
| Reglas de arquitectura | `src/test/.../ArquitecturaTest.java` |

La recomendación está en `servicios/recomendacion/` (ver su [README](../servicios/recomendacion/README.md)), y las solicitudes, en `servicios/nucleo/` (ver su [README](../servicios/nucleo/README.md)).

Fuera del código: `infra/docker-compose.yml`, `infra/nginx/nginx.conf`, `infra/mongo/init/` (vacío: los datos de demostración los carga `CargaDemoMongo` al arrancar), `.github/workflows/ci.yml`.

## 7. Resumen por capa

- **Dominio:** *qué* se puede hacer y bajo qué reglas.
- **Aplicación:** *en qué orden* se hacen los pasos.
- **Infraestructura:** *con qué tecnología*.
- **Nginx:** *a qué servicio y réplica* va cada petición.
- **Avisos:** cómo el núcleo le cuenta al catálogo lo que decidió, sin compartir bases de datos.
