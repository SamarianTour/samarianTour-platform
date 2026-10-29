package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

/** El correo y la contraseña (RF-04). */
public record InicioSesionRequest(String correo, String contrasena) {

    @Override
    public String toString() {
        return "InicioSesionRequest[correo=%s]".formatted(correo);
    }
}
