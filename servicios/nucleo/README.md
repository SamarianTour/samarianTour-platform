# nucleo

Núcleo transaccional sobre PostgreSQL: cuentas, solicitudes de disponibilidad, pagos, prestadores, administración e indicadores. Las decisiones están en [docs/decisiones-nucleo.md](../../docs/decisiones-nucleo.md) (DN-01 en adelante).

Por ahora tiene la base del servicio, la bandeja de salida hacia el catálogo, la moderación de ofertas y opiniones (DN-08), las solicitudes de disponibilidad (DN-09 a DN-17), los pagos (DN-18 a DN-27), las cuentas (DN-28 a DN-32) los prestadores con la sincronización del RNT (DN-33 a DN-36) y la validación del token de acceso con Spring Security y `libs/seguridad-jwt` (DN-37). Los indicadores están vacíos, con un `package-info.java` que dice qué requisitos le tocan.

## Estructura

```
co.edu.unimagdalena.samariantour.nucleo
├── NucleoApplication
├── compartido/          identidad del token y reglas por prefijo (DN-04, DN-05, DN-37), reloj, X-Instancia, reintento
│                        del ADR-05, cliente del catálogo, cifrado de los datos personales (DN-28)
├── cuentas/             módulo 1 (RF-01 a RF-13): registro, sesión con JWT, restablecimiento, datos personales,
│                        supresión y su api para los demás módulos
├── solicitudes/         módulo 4 (RF-30 a RF-45): ciclo de vida, ventanas, idempotencia, notificaciones y su api para pagos
├── pagos/               módulo 5 (RF-46 a RF-58): sesión de cobro, webhook, pagos tardíos, comisión y dispersión
├── prestadores/         módulo 7 (RF-62, RF-63, RF-68 a RF-75): registro con el RNT, tipo, WhatsApp, cuenta de cobro,
│                        sincronización del RNT y su api para los demás módulos
├── administracion/      módulo 8 (RF-76 a RF-83); por ahora la sincronización del RNT (RF-76), la moderación (RF-79,
│                        RF-80), la comisión y los pagos tardíos
├── indicadores/         módulo 9 (RF-85 a RF-89)
└── bandeja/             bandeja de salida hacia el catálogo (DN-06)
```

Cada módulo es hexagonal como en el catálogo (`dominio`, `aplicacion`, `infraestructura`; ver el README principal). Las entidades JPA y los repositorios van en `infraestructura/salida/persistencia` (DN-02), y las tablas, en una migración de Flyway nueva en `src/main/resources/db/migration` (DN-01). `ArquitecturaTest` verifica las reglas en cada build.

## Compilar y probar

```bash
mvn -pl servicios/nucleo -am verify
```

Las pruebas de integración (`*IT`) usan un PostgreSQL de Testcontainers (y `RestablecimientoIT`, un Mailpit), así que necesitan Docker. Generan sus propias claves de cifrado; los tokens los firma `TokensDePrueba`, del test-jar de `libs/seguridad-jwt`, con un par de claves que se crea al arrancar. En Windows hay que correrlas desde PowerShell: desde Git Bash, Testcontainers no encuentra Docker.

## Seguridad

Las rutas `/turista/**`, `/prestador/**`, `/admin/**`, `/operador/**` y `/gestor/**` exigen el token de acceso con su rol, y `/cuenta/**` el de cualquiera (DN-05, DN-37). Las reglas están en `compartido/config/SeguridadConfig`, con Spring Security y `libs/seguridad-jwt`; los controladores leen `Identidad.de(peticion)`. Sin token válido, 401 `NO_AUTENTICADO`; con otro rol, 403 `ACCESO_DENEGADO`.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/nucleo/cuentas/sesion -H "Content-Type: application/json" \
  -d '{"correo":"laura.gomez@demo.samariantour.co","contrasena":"Demo1234"}' | sed -E 's/.*"tokenAcceso":"([^"]+)".*/\1/')
curl -s http://localhost:8080/api/nucleo/cuenta -H "Authorization: Bearer $TOKEN"
```

- **El núcleo valida sus propios tokens** con la clave pública que sale de `NUCLEO_JWT_CLAVE_PRIVADA`; el catálogo y la recomendación, con `NUCLEO_JWT_CLAVE_PUBLICA`.
- **El tipo del prestador** lo aporta el módulo de prestadores por `cuentas.api.FuenteTipoPrestador`, sin que las cuentas dependan de él. Tras registrar el RNT o cambiar de tipo, renovar la sesión trae el token con el tipo.
- **Pruebas:** `AutorizacionPorRutaTest` es la batería de acceso cruzado de QAS-04 (cada rol en cada prefijo, tokens vencidos, de otra clave o de otro emisor, rutas con `;`). Las pruebas firman los tokens con `TokensDePrueba.bearer(usuario, rol)`.
- **Pendiente:** la autenticación entre servicios en `/internal/**` y el TLS de Nginx (DN-37).

## Enviar un aviso al catálogo

Un módulo que tiene que avisar al catálogo declara su propio puerto de salida y lo implementa con la bandeja. Así lo hace Cuentas cuando suprime los datos de un turista (RF-13, DC-45 del catálogo; `AvisosSupresionBandejaAdapter`):

```java
// cuentas/dominio/puerto/salida/AvisosCatalogoPort.java
public interface AvisosCatalogoPort {
    void avisarSupresion(UUID turistaId);
}

// cuentas/infraestructura/salida/catalogo/AvisosCatalogoAdapter.java
@Component
class AvisosCatalogoAdapter implements AvisosCatalogoPort {

    private final BandejaSalidaApi bandeja;

    AvisosCatalogoAdapter(BandejaSalidaApi bandeja) {
        this.bandeja = bandeja;
    }

    @Override
    public void avisarSupresion(UUID turistaId) {
        String idAviso = "supresion-" + turistaId;   // estable: reenviar el mismo cambio da el mismo id
        bandeja.encolar(idAviso, DestinoAviso.OPINIONES,
                Map.of("idAviso", idAviso, "tipo", "SUPRESION_TURISTA", "turistaId", turistaId));
    }
}
```

El servicio de aplicación llama al puerto dentro de su método `@Transactional`: el aviso se guarda en la misma transacción que la supresión, y la bandeja lo envía al catálogo en los siguientes 15 s, reintentando si el catálogo no responde. Fuera de una transacción, `encolar` falla. Los avisos y sus campos están en `contratos/catalogo.openapi.yaml` (`POST /internal/avisos` y `POST /internal/opiniones/avisos`).

## Solicitudes de disponibilidad

El turista envía la solicitud con su `Idempotency-Key`, el operador la atiende y el turista confirma (DN-09 a DN-16). El recorrido completo con `curl` está en el README principal ("12. Solicitudes de disponibilidad"). `$TOKEN` es el token de acceso de la turista (ver "Seguridad"):

```bash
curl -s -X POST http://localhost:8080/api/nucleo/turista/solicitudes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: 11111111-aaaa-4bbb-8ccc-000000000001" -H "Content-Type: application/json" \
  -d "{\"ofertaId\":\"b00000000000000000000001\",\"fechaInicio\":\"$(date -d '+3 days' +%F)\",\"horaSalida\":\"07:00\",\"personas\":2}"
```

- **Campos de cada tipo de oferta** (`CamposDelTipo`, RF-30): la actividad y el paquete piden `horaSalida`; el alojamiento, `fechaSalida` y `numeroUnidades`; el restaurante, `horaReserva`. Un campo que falta o que no corresponde al tipo es 400.
- **Ventanas** (`PoliticaVentanas`): 24 h para el operador; 3 h para confirmar y pagar desde que se atiende, o hasta las 08:00 si se atiende entre las 19:00 y las 04:59; 30 min de gracia con un pago en curso. Están en el dominio a propósito (QAS-05).
- **Tareas programadas:** los vencimientos cada minuto (con el cierre de DN-36), el envío de notificaciones cada 10 s y la limpieza de claves de idempotencia cada hora (`nucleo.solicitudes.*.cron`; con `-` no corren). Las dos réplicas trabajan sin pisarse con `FOR UPDATE SKIP LOCKED`.
- **Prestadores y turistas:** el operador y el WhatsApp de cada prestador salen de `prestadores.api.PrestadoresApiInterna` (DN-35), y el contacto del turista, de `cuentas.api.CuentasApiInterna` (DN-32): solo puede enviar solicitudes con el WhatsApp verificado. Si el prestador pierde el RNT activo, sus solicitudes no pagadas se cierran en el minuto siguiente (DN-36).
- **Notificaciones:** por ahora, WhatsApp simulado en el log, con el número enmascarado: `docker compose -f infra/docker-compose.yml logs nucleo | grep "WhatsApp simulado"`.
- **Para pagos:** `solicitudes.api.SolicitudesPagosApi` (DN-17), que se llama dentro de la transacción de pagos.

## Pagos

El turista abre el pago de una solicitud confirmada, paga en la pasarela y el resultado llega por el webhook (DN-18 a DN-27). El recorrido completo, con los rechazos, el pago tardío y la dispersión fallida, está en el README principal ("13. Pagos"):

```bash
curl -s -X POST http://localhost:8080/api/nucleo/turista/solicitudes/<id>/pago   -H "Authorization: Bearer $TOKEN"   -H "Idempotency-Key: 33333333-aaaa-4bbb-8ccc-000000000001"
# 201 {"numero":1,"estado":"EN_CURSO","montoCentavos":30000000,"checkout":{"pasarela":"SIMULADA","url":"..."},...}
```

- **La pasarela** se elige con `nucleo.pagos.pasarela`: `simulada` (por defecto, una página del núcleo que envía al webhook eventos con la forma y la firma de Wompi, solo con el perfil demo) o `wompi` (el sandbox, con las llaves en `infra/.env`; ver "Pasarela de pagos" en el README principal). Las dos están detrás de `PaymentGatewayPort` (QAS-06).
- **El webhook** `POST /pagos/eventos` es público: verifica el checksum (401 si no coincide), guarda el evento y responde 200 de inmediato. Una tarea lo procesa después, con la solicitud en la misma transacción (`SolicitudesPagosApi`, DN-17).
- **Tareas programadas** (`nucleo.pagos.*.cron`; con `-` no corren): los eventos cada 5 s, los intentos abandonados cada minuto, las dispersiones y los avisos cada 15 s y las claves de idempotencia vencidas cada hora. Las dos réplicas trabajan sin pisarse con `FOR UPDATE SKIP LOCKED`.
- **Comisión y dispersión:** el 4 % inicial, con historial (`/admin/comision`), y el costo transaccional configurable (`nucleo.pagos.costo-transaccional`, DN-23). La dispersión se reintenta 3 veces; con Wompi también es simulada, porque "Pagos a terceros" exige activarlo en la cuenta del comercio (DN-24).
- **Cuentas de cobro:** salen de `PrestadoresApiInterna` (DN-35); fuera de la dispersión solo se ven enmascaradas (RF-75). El WhatsApp del administrador sigue en `application-demo.yml` (DN-26).
- **Pruebas:** `PagosIT` recorre los pagos con PostgreSQL y eventos firmados; `PasarelaSimuladaIT`, la página y su envío al webhook; `FormatoWompiTest`, las firmas con los vectores de la documentación de Wompi.

## Cuentas

Registro, sesión, restablecimiento de la contraseña, datos personales y supresión (DN-28 a DN-32). El recorrido completo con `curl` está en el README principal ("14. Cuentas"). Lo esencial:

- **Claves:** el núcleo no arranca sin `NUCLEO_CIFRADO_CLAVE` (cifra el nombre y el WhatsApp) ni `NUCLEO_JWT_CLAVE_PRIVADA` (firma el token). `bash infra/generar-claves.sh` las agrega a `infra/.env` solo si faltan, y deriva de la privada la pública que usan el catálogo y la recomendación (DN-37). **No cambies la clave de cifrado de una base con datos**: lo cifrado con la anterior deja de leerse.
- **Cuentas de la demo:** las 13 de `PrecargaCuentasDemo`, con la clave `Demo1234` (por ejemplo `laura@demo.samariantour.co`); el administrador de V9 es `admin@samariantour.co` / `SamarianAdmin2026`.
- **Sesión:** `POST /cuentas/sesion` da un JWT RS256 de 15 min y un token de renovación de 7 días que rota en cada uso (DN-29). El token del prestador lleva su tipo cuando ya registró el RNT (DN-37).
- **WhatsApp y correo simulados:** el código de verificación sale en el log (`docker compose -f infra/docker-compose.yml logs nucleo | grep CODIGO_VERIFICACION`) y el correo de restablecimiento llega a Mailpit, en `http://localhost:8025`.
- **Para otros módulos:** `cuentas.api.CuentasApiInterna`. Las cuentas no dependen de ningún módulo.

## Prestadores

Registro con el RNT, tipo, WhatsApp verificado, cuenta de cobro y sincronización semanal con el RNT (DN-33 a DN-36). El recorrido completo con `curl` está en el README principal ("15. Prestadores"). Lo esencial:

- **El RNT** se consulta en una tabla simulada (`nucleo.rnt.fuente=simulada`, por defecto) o en la API SODA de datos.gov.co (`soda`). La simulada está en `application-demo.yml`: 910001 a 910004 son los prestadores de la demo, 910005 no está (su agencia quedó inactiva), y 910006 (guía) y 910007 (hostal) sirven para registrar uno nuevo.
- **El tipo tiene que coincidir con la categoría del RNT** (409 `TIPO_NO_COINCIDE_CON_RNT`); el guía, además, debe ser persona natural (RF-70).
- **Habilitación:** solo con el RNT ACTIVO se publica y se aprueban ofertas (409 `PRESTADOR_NO_HABILITADO` en la moderación). Cada cambio llega al catálogo con el aviso `HABILITACION` por la bandeja.
- **Sincronización** (RF-76 a RF-78): los lunes a las 3:00 (`nucleo.administracion.sincronizacion-rnt.cron`) o con `POST /admin/rnt/sincronizacion`. Para la demo, `DELETE /admin/rnt-simulado/910001` da de baja el RNT de la agencia y sincroniza: queda INACTIVA, el catálogo retira sus ofertas, recibe un WhatsApp (`grep CATALOGO_RETIRADO_RNT`) y sus solicitudes no pagadas se cierran en el minuto siguiente. `PUT` lo restituye. Las bajas quedan en PostgreSQL (`rnt_simulado_bajas`), así las dos réplicas las ven.
- **WhatsApp simulado:** el código de verificación sale en el log (`grep CODIGO_VERIFICACION_PRESTADOR`).
- **Para otros módulos:** `prestadores.api.PrestadoresApiInterna`. Los prestadores solo dependen de `cuentas.api`.

## Moderar

Con el piloto levantado, el administrador decide sobre la versión que le muestra la lista de moderación del catálogo. `$TOKEN_ADMIN` es su token, que se pide como en "Seguridad" con `admin@samariantour.co` y `SamarianAdmin2026`:

```bash
curl -s -X POST http://localhost:8080/api/nucleo/admin/moderacion/ofertas/b00000000000000000000017/decision   -H "Authorization: Bearer $TOKEN_ADMIN"   -H "Content-Type: application/json" -d '{"numeroVersion": 1, "decision": "APROBAR"}'
```

La decisión queda en `decisiones_moderacion` y su aviso, en la bandeja; en los siguientes 15 s la oferta aparece en el catálogo. Los errores (404, 409, 503) y las decisiones repetidas están en DN-08 y en `contratos/nucleo.openapi.yaml`. Las notificaciones de rechazo, por ahora, solo salen en el log: `docker compose -f infra/docker-compose.yml logs nucleo | grep "Notificación simulada"`.
