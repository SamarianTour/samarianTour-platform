# Scripts de base de datos · Samarian Tour

Samarian Tour usa **persistencia políglota** (ADR-03): el núcleo transaccional guarda sus datos en PostgreSQL 17 y el
catálogo en MongoDB 7, con las fotos en MinIO. La recomendación no tiene base de datos.

| Archivo | Base | Qué contiene |
|---|---|---|
| `01-nucleo-postgresql.sql` | PostgreSQL 17, base `nucleo` | 25 tablas, 59 índices y los datos base (política de datos `v1` y comisión inicial del 4 %). Es el resultado de las migraciones V1 a V17. |
| `02-catalogo-mongodb.js` | MongoDB 7, base `catalogo_db` | 10 colecciones y sus 16 índices, incluidos los parciales de la búsqueda. |

## Cómo se crea la base normalmente

No hace falta correr los scripts. Con Docker Compose (`docker compose -f infra/docker-compose.yml up -d --build`):

- **PostgreSQL:** al arrancar, el núcleo aplica las migraciones de Flyway que están en
  `servicios/nucleo/src/main/resources/db/migration` (V1 a V17, más V9 en Java) y registra cada una en
  `flyway_schema_history`. El esquema siempre queda en la última versión.
- **MongoDB:** al arrancar, el catálogo crea sus índices (`IndicesMongo` e `IndicesOpinionesMongo`); las colecciones
  aparecen con el primer documento o el primer índice.
- **Datos de la demostración:** el perfil `demo` (activo en Docker Compose) carga las cuentas demo, los prestadores, las
  zonas, las ofertas con sus fotos y las opiniones.

## Cómo usar los scripts a mano

```bash
# PostgreSQL
createdb -U postgres nucleo
psql -U postgres -d nucleo -f 01-nucleo-postgresql.sql

# MongoDB
mongosh "mongodb://localhost:27017/catalogo_db" 02-catalogo-mongodb.js
```

**Una base creada con el script SQL no se mezcla con Flyway:** el script no crea `flyway_schema_history`, así que si
después arranca el núcleo sobre esa base, Flyway intentará aplicar las migraciones otra vez. Para correr la aplicación,
deje la base vacía y que el núcleo la prepare.

## El administrador inicial

La cuenta del administrador la crea la migración **V9**, que está en Java y no en SQL porque el nombre se guarda
cifrado con AES-256-GCM, con la clave propia de cada instalación (`NUCLEO_CIFRADO_CLAVE` en `infra/.env`), y la
contraseña con BCrypt. Por eso no puede ir en un script fijo.

- Correo: `admin@samariantour.co`
- Contraseña inicial: la del README del repositorio. Se cambia con el restablecimiento por correo (RF-07) al instalar,
  como indica la guía de despliegue.

## Cómo se generaron y verificaron (17 de noviembre de 2026)

- **PostgreSQL:** las 16 migraciones SQL se aplicaron en orden sobre un PostgreSQL 17 vacío y se exportó el resultado
  con `pg_dump --column-inserts`. Después, el script se corrió sobre otra base vacía y dejó las mismas 25 tablas y
  59 índices.
- **MongoDB:** el servicio de catálogo, compilado desde el código actual, arrancó contra un MongoDB 7 vacío con los
  perfiles `mongo` y `demo`. Los índices que creó se compararon con los del script: no hay diferencias.
