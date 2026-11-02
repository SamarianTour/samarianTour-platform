package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Las cuentas internas, que guarda el módulo de cuentas (RF-81, DN-40). */
public interface CuentasInternasPort {

    List<CuentaInterna> internas();

    /** Vacío si no existe o no es interna. */
    Optional<CuentaInterna> porId(UUID usuarioId);

    /** Sin contraseña: la invitación sale por correo en la misma transacción. */
    CuentaInterna crear(String rol, String nombre, String correo);

    CuentaInterna desactivar(UUID usuarioId);

    CuentaInterna reactivar(UUID usuarioId);

    boolean esOperadorActivo(UUID usuarioId);
}
