package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * La cuenta como la ve su titular. Nunca lleva la contraseña ni su hash.
 *
 * @param whatsapp           solo el turista
 * @param whatsappVerificado solo el turista: si ya puede enviar solicitudes (RF-30)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CuentaResponse(UUID id, String rol, String nombre, String correo, String whatsapp,
                             Boolean whatsappVerificado) {
}
