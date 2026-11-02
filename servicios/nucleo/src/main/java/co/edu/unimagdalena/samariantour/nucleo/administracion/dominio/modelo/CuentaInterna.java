package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Una cuenta interna, como la dan las cuentas (RF-81).
 *
 * @param rol                   ADMINISTRADOR, OPERADOR_DISPONIBILIDAD o GESTOR_DESTINO
 * @param estado                ACTIVA o DESACTIVADA
 * @param contrasenaEstablecida false mientras no use el enlace de la invitación
 */
public record CuentaInterna(UUID id, String rol, String nombre, String correo, String estado,
                            boolean contrasenaEstablecida, Instant registradaEn) {

    public static final String OPERADOR = "OPERADOR_DISPONIBILIDAD";

    public boolean esOperador() {
        return OPERADOR.equals(rol);
    }
}
