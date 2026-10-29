package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto;

import java.time.Instant;

/** La cuenta de cobro vigente, con el número enmascarado: completo solo lo usa la dispersión (RF-75). */
public record CuentaCobroResponse(String tipo, String entidad, String numeroEnmascarado, String titular,
                                  Instant vigenteDesde) {
}
