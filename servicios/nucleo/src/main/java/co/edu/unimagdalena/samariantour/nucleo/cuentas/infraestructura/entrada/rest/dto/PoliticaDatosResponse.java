package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

import java.time.LocalDate;

/**
 * La versión vigente de la política de datos (RF-09 a RF-11).
 *
 * @param urlTexto dónde se lee el texto completo, en Markdown
 */
public record PoliticaDatosResponse(String version, LocalDate fechaPublicacion, String urlTexto) {
}
