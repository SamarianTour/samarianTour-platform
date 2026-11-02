package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una cuenta interna, como la ve la administración (RF-81). Nunca lleva la contraseña.
 *
 * @param rol                   ADMINISTRADOR, OPERADOR_DISPONIBILIDAD o GESTOR_DESTINO
 * @param estado                ACTIVA o DESACTIVADA
 * @param contrasenaEstablecida false mientras no use el enlace de la invitación
 */
public record CuentaInternaRegistrada(UUID id, String rol, String nombre, String correo, String estado,
                            boolean contrasenaEstablecida, Instant registradaEn) {

    public CuentaInternaRegistrada {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(estado, "estado");
    }
}
