package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que comparten las cuentas de los cinco roles, para crear o reconstruir un {@link Usuario}.
 *
 * @param hashContrasena vacío en una cuenta suprimida o en una interna que todavía no tiene contraseña (RF-81)
 * @param bloqueadaHasta vacío si no hay bloqueo (RF-05)
 * @param revision       la versión guardada, para el bloqueo optimista (ADR-05); vacía en una cuenta nueva
 * @param supresion      vacía si el titular no pidió suprimir sus datos (RF-13)
 */
public record DatosCuenta(
        UUID id,
        String nombre,
        String correo,
        String hashContrasena,
        EstadoCuenta estado,
        int intentosFallidos,
        Instant bloqueadaHasta,
        Instant fechaRegistro,
        Long revision,
        List<AceptacionPolitica> aceptaciones,
        SolicitudSupresion supresion) {

    public DatosCuenta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(correo, "correo");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fechaRegistro, "fechaRegistro");
        aceptaciones = List.copyOf(aceptaciones);
    }

    /** Una cuenta recién creada: activa, sin intentos fallidos y sin aceptaciones todavía. */
    public static DatosCuenta nueva(UUID id, String nombre, String correo, String hashContrasena, Instant ahora) {
        return new DatosCuenta(id, Validaciones.nombre(nombre), Validaciones.correo(correo), hashContrasena,
                EstadoCuenta.ACTIVA, 0, null, ahora, null, List.of(), null);
    }
}
