package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Responde 401 en formato ProblemDetail (RFC 7807), con el código {@code NO_AUTENTICADO}, cuando falta
 * la identidad del usuario.
 */
@RestControllerAdvice
public class ManejadorNoAutenticado {

    public static final String CODIGO = "NO_AUTENTICADO";

    @ExceptionHandler(NoAutenticadoException.class)
    ProblemDetail noAutenticado(NoAutenticadoException excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, excepcion.getMessage());
        problema.setTitle("No autenticado");
        problema.setProperty("codigo", CODIGO);
        return problema;
    }
}
