package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.util.Objects;

/** Una clave de idempotencia ya usada: qué operación y qué petición la usaron, y qué se respondió (DN-19). */
public record OperacionRegistrada(String operacion, String huella, RespuestaGuardada respuesta) {

    public OperacionRegistrada {
        Objects.requireNonNull(operacion, "operacion");
        Objects.requireNonNull(huella, "huella");
        Objects.requireNonNull(respuesta, "respuesta");
    }

    public boolean coincideCon(PeticionIdempotente peticion) {
        return operacion.equals(peticion.operacion()) && huella.equals(peticion.huella());
    }
}
