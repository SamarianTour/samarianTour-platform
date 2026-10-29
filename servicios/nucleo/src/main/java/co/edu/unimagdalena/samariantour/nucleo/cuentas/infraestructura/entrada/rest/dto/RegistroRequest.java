package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

/**
 * Lo que envía quien se registra (RF-01 a RF-03, RF-09, RF-10).
 *
 * @param rol             TURISTA o PRESTADOR
 * @param whatsapp        solo el turista, con indicativo: +573001234567
 * @param aceptaPolitica  true: la aceptación explícita de la política (RF-09)
 * @param versionPolitica la versión que leyó, de {@code GET /politica-datos/vigente}
 */
public record RegistroRequest(String rol, String nombre, String correo, String contrasena, String whatsapp,
                              Boolean aceptaPolitica, String versionPolitica) {

    @Override
    public String toString() {
        return "RegistroRequest[rol=%s, correo=%s]".formatted(rol, correo);
    }
}
