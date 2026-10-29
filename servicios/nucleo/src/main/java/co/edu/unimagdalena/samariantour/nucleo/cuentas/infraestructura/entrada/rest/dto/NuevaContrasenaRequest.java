package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

/**
 * La contraseña nueva y el token del enlace que llegó por correo (RF-08).
 *
 * @param token el parámetro {@code token} del enlace
 */
public record NuevaContrasenaRequest(String token, String contrasena) {

    @Override
    public String toString() {
        return "NuevaContrasenaRequest[token=******]";
    }
}
