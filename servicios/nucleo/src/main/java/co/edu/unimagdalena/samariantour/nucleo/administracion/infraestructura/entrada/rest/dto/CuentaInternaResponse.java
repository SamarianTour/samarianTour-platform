package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Una cuenta interna; nunca lleva la contraseña (RF-81).
 *
 * @param estado                ACTIVA o DESACTIVADA
 * @param contrasenaEstablecida false mientras no use el enlace de la invitación
 */
public record CuentaInternaResponse(UUID id, String rol, String nombre, String correo, String estado,
                                    boolean contrasenaEstablecida, Instant registradaEn) {
}
