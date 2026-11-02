package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;

/**
 * Los datos de una cuenta interna nueva (RF-01, RF-81).
 *
 * @param rol ADMINISTRADOR, OPERADOR_DISPONIBILIDAD o GESTOR_DESTINO
 */
public record CrearCuentaInternaCommand(Rol rol, String nombre, String correo) {

    @Override
    public String toString() {
        return "CrearCuentaInternaCommand[rol=%s, correo=%s]".formatted(rol, correo);
    }
}
