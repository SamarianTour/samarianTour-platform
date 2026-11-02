package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Un prestador para asignarle el operador (RF-81).
 *
 * @param razonSocial la del RNT; se omite mientras no se encuentre
 * @param operadorId  el operador que lo atiende; se omite si no tiene
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrestadorAdminResponse(UUID id, String numeroRnt, String razonSocial, String tipo, String estadoRnt,
                                     UUID operadorId, Instant registradoEn) {
}
