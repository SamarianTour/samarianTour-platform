package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

/**
 * Los datos personales que se reemplazan (RF-12).
 *
 * @param whatsapp obligatorio para el turista; el prestador y las cuentas internas no lo envían
 */
public record DatosPersonalesRequest(String nombre, String whatsapp) {
}
