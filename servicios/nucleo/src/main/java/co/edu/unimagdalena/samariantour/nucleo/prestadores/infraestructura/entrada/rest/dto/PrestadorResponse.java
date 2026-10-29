package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Los datos del prestador como los ve él mismo (RF-63, RF-68, RF-69, RF-71, RF-73). Nunca salen en el catálogo.
 *
 * @param razonSocial          y {@code municipio}: los del RNT; vacíos mientras no se haya encontrado
 * @param ultimaSincronizacion la última vez que el RNT respondió (RF-73)
 * @param habilitado           si puede publicar: solo con el RNT activo (RF-77, RF-79)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrestadorResponse(UUID id, String numeroRnt, String tipo, Integer numeroEmpleados,
                                List<String> modalidades, List<String> idiomas, String categoria, String razonSocial,
                                String municipio, String estadoRnt, Instant ultimaSincronizacion, boolean habilitado,
                                String whatsapp, Boolean whatsappVerificado) {
}
