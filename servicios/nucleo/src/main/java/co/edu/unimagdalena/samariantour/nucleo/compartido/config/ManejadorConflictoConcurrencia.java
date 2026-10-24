package co.edu.unimagdalena.samariantour.nucleo.compartido.config;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Un conflicto de concurrencia que no se resolvió con los reintentos responde 409 (ADR-05). El cliente puede
 * reintentar; el núcleo reenvía el aviso más tarde. Atiende a todos los módulos del servicio.
 */
@RestControllerAdvice
public class ManejadorConflictoConcurrencia {

    @ExceptionHandler(ConflictoConcurrenciaException.class)
    ProblemDetail conflicto(ConflictoConcurrenciaException excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Otra operación modificó el mismo dato al mismo tiempo; vuelve a intentarlo");
        problema.setTitle("Conflicto de concurrencia");
        problema.setProperty("codigo", ConflictoConcurrenciaException.CODIGO);
        return problema;
    }
}
