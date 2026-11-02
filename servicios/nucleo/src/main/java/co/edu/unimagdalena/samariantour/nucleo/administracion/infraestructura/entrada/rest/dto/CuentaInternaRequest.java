package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

/** @param rol ADMINISTRADOR, OPERADOR_DISPONIBILIDAD o GESTOR_DESTINO */
public record CuentaInternaRequest(String rol, String nombre, String correo) {
}
