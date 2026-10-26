# nucleo

Núcleo transaccional sobre PostgreSQL: cuentas, solicitudes de disponibilidad, pagos, prestadores, administración e indicadores. Las decisiones están en [docs/decisiones-nucleo.md](../../docs/decisiones-nucleo.md) (DN-01 en adelante).

Por ahora tiene la base del servicio, la bandeja de salida hacia el catálogo, la moderación de ofertas y opiniones (DN-08) y las solicitudes de disponibilidad (DN-09 a DN-17). Los demás módulos están vacíos, cada uno con un `package-info.java` que dice qué requisitos le tocan.

## Estructura

```
co.edu.unimagdalena.samariantour.nucleo
├── NucleoApplication
├── compartido/          identidad y roles (DN-04, DN-05), reloj, X-Instancia, reintento del ADR-05, cliente del catálogo
├── cuentas/             módulo 1 (RF-01 a RF-13)
├── solicitudes/         módulo 4 (RF-30 a RF-45): ciclo de vida, ventanas, idempotencia, notificaciones y su api para pagos
├── pagos/               módulo 5 (RF-46 a RF-58)
├── prestadores/         módulo 7 (RF-62, RF-63, RF-68 a RF-71, RF-73 a RF-75)
├── administracion/      módulo 8 (RF-76 a RF-83); por ahora la moderación (RF-79, RF-80)
├── indicadores/         módulo 9 (RF-85 a RF-89)
└── bandeja/             bandeja de salida hacia el catálogo (DN-06)
```

Cada módulo es hexagonal como en el catálogo (`dominio`, `aplicacion`, `infraestructura`; ver el README principal). Las entidades JPA y los repositorios van en `infraestructura/salida/persistencia` (DN-02), y las tablas, en una migración de Flyway nueva en `src/main/resources/db/migration` (DN-01). `ArquitecturaTest` verifica las reglas en cada build.

## Compilar y probar

```bash
mvn -pl servicios/nucleo -am verify
```

Las pruebas de integración (`*IT`) usan un PostgreSQL de Testcontainers, así que necesitan Docker. En Windows hay que correrlas desde PowerShell: desde Git Bash, Testcontainers no encuentra Docker.

## Enviar un aviso al catálogo

Un módulo que tiene que avisar al catálogo, por ejemplo Cuentas cuando suprime los datos de un turista (RF-13, DC-45 del catálogo), declara su propio puerto de salida y lo implementa con la bandeja:

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

El turista envía la solicitud con su `Idempotency-Key`, el operador la atiende y el turista confirma (DN-09 a DN-16). El recorrido completo con `curl` está en el README principal ("12. Solicitudes de disponibilidad"):

```bash
curl -s -X POST http://localhost:8080/api/nucleo/turista/solicitudes \
  -H "X-Usuario-Id: 90000000-0000-0000-0000-000000000001" -H "X-Rol: TURISTA" \
  -H "Idempotency-Key: 11111111-aaaa-4bbb-8ccc-000000000001" -H "Content-Type: application/json" \
  -d "{\"ofertaId\":\"b00000000000000000000001\",\"fechaInicio\":\"$(date -d '+3 days' +%F)\",\"horaSalida\":\"07:00\",\"personas\":2}"
```

- **Campos de cada tipo de oferta** (`CamposDelTipo`, RF-30): la actividad y el paquete piden `horaSalida`; el alojamiento, `fechaSalida` y `numeroUnidades`; el restaurante, `horaReserva`. Un campo que falta o que no corresponde al tipo es 400.
- **Ventanas** (`PoliticaVentanas`): 24 h para el operador; 3 h para confirmar y pagar desde que se atiende, o hasta las 08:00 si se atiende entre las 19:00 y las 04:59; 30 min de gracia con un pago en curso. Están en el dominio a propósito (QAS-05).
- **Tareas programadas:** los vencimientos cada minuto, el envío de notificaciones cada 10 s y la limpieza de claves de idempotencia cada hora (`nucleo.solicitudes.*.cron`; con `-` no corren). Las dos réplicas trabajan sin pisarse con `FOR UPDATE SKIP LOCKED`.
- **Datos provisionales:** el operador de cada prestador y el WhatsApp de prestadores y turistas están en `application-demo.yml` (DN-15) hasta que existan los módulos de prestadores y cuentas.
- **Notificaciones:** por ahora, WhatsApp simulado en el log, con el número enmascarado: `docker compose -f infra/docker-compose.yml logs nucleo | grep "WhatsApp simulado"`.
- **Para pagos:** `solicitudes.api.SolicitudesPagosApi` (DN-17), que se llama dentro de la transacción de pagos.

## Moderar

Con el piloto levantado, el administrador decide sobre la versión que le muestra la lista de moderación del catálogo:

```bash
curl -s -X POST http://localhost:8080/api/nucleo/admin/moderacion/ofertas/b00000000000000000000017/decision   -H "X-Usuario-Id: 60000000-0000-0000-0000-000000000006" -H "X-Rol: ADMINISTRADOR"   -H "Content-Type: application/json" -d '{"numeroVersion": 1, "decision": "APROBAR"}'
```

La decisión queda en `decisiones_moderacion` y su aviso, en la bandeja; en los siguientes 15 s la oferta aparece en el catálogo. Los errores (404, 409, 503) y las decisiones repetidas están en DN-08 y en `contratos/nucleo.openapi.yaml`. Las notificaciones de rechazo, por ahora, solo salen en el log: `docker compose -f infra/docker-compose.yml logs nucleo | grep "Notificación simulada"`.
