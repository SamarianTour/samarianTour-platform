package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.util.Objects;

/** Una clave de idempotencia ya usada: qué operación y qué petición la usaron, y qué se respondió (DN-12). */
public record OperacionRegistrada(String operacion, String huella, RespuestaGuardada respuesta) {

    public OperacionRegistrada {
        Objects.requireNonNull(operacion, "operacion");
        Objects.requireNonNull(huella, "huella");
        Objects.requireNonNull(respuesta, "respuesta");
    }

    /** Si la petición nueva es la misma que usó la clave: misma operación y misma huella. */
    public boolean coincideCon(PeticionIdempotente peticion) {
        return operacion.equals(peticion.operacion()) && huella.equals(peticion.huella());
    }
}
