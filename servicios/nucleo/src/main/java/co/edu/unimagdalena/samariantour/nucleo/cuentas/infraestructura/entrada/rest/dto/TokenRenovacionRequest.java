package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

/** El token de renovación que se recibió al iniciar o al renovar la sesión (DN-19). */
public record TokenRenovacionRequest(String tokenRenovacion) {

    @Override
    public String toString() {
        return "TokenRenovacionRequest[tokenRenovacion=******]";
    }
}
