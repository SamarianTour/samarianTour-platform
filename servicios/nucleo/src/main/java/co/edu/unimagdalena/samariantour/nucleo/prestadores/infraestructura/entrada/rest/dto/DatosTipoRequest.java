package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto;

import java.util.List;

/** El tipo y sus campos condicionales, como en el registro (RF-68, RF-69). */
public record DatosTipoRequest(String tipo, Integer numeroEmpleados, List<String> modalidades, List<String> idiomas) {
}
