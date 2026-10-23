package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores de seguridad en formato ProblemDetail (RFC 7807), con la propiedad {@code codigo} (DC-15): 401
 * {@code NO_AUTENTICADO} cuando falta la identidad y 403 {@code ACCESO_DENEGADO} cuando el rol no corresponde a la
 * ruta.
 */
@RestControllerAdvice
public class ManejadorErroresSeguridad {

    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";

    @ExceptionHandler(NoAutenticadoException.class)
    ProblemDetail noAutenticado(NoAutenticadoException excepcion) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", NO_AUTENTICADO, excepcion.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ProblemDetail accesoDenegado(AccesoDenegadoException excepcion) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", ACCESO_DENEGADO, excepcion.getMessage());
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
