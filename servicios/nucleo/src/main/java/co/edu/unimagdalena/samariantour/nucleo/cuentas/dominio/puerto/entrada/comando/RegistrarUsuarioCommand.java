package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;

/**
 * Los datos del registro (RF-01 a RF-03, RF-09).
 *
 * @param whatsapp        solo el turista
 * @param aceptaPolitica  la aceptación explícita de la política de datos (RF-09)
 * @param versionPolitica la versión que el usuario leyó y aceptó (RF-10)
 */
public record RegistrarUsuarioCommand(Rol rol, String nombre, String correo, String contrasena, String whatsapp,
                                      boolean aceptaPolitica, String versionPolitica) {

    @Override
    public String toString() {
        return "RegistrarUsuarioCommand[rol=%s, correo=%s]".formatted(rol, correo);
    }
}
