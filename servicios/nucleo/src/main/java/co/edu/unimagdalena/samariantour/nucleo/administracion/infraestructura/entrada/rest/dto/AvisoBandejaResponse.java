package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import java.time.Instant;

/** Un aviso de la bandeja de salida hacia el catálogo (DN-44). */
public record AvisoBandejaResponse(String idAviso, String destino, String tipo, String estado, int intentos,
                                   String ultimoError, Instant creadoEn) {
}
