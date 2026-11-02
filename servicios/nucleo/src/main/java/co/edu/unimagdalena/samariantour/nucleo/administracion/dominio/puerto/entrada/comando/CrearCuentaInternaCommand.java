package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando;

/** @param rol ADMINISTRADOR, OPERADOR_DISPONIBILIDAD o GESTOR_DESTINO */
public record CrearCuentaInternaCommand(String rol, String nombre, String correo) {

    @Override
    public String toString() {
        return "CrearCuentaInternaCommand[rol=%s, correo=%s]".formatted(rol, correo);
    }
}
