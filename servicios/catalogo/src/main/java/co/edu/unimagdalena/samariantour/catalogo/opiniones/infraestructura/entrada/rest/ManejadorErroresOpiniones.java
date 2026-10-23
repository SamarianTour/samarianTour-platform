package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoHabilitadaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio de opiniones a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, como
 * el manejador del catálogo (DC-15). Un JSON ilegible lo responde el manejador del catálogo, que vale para todas las
 * rutas del servicio.
 */
@RestControllerAdvice
public class ManejadorErroresOpiniones {

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estadoDe(excepcion), excepcion.getMessage());
        problema.setProperty("codigo", excepcion.codigo());
        return problema;
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case OpinionNoHabilitadaException e -> HttpStatus.FORBIDDEN;
            case OfertaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case OpinionNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case OpinionDuplicadaException e -> HttpStatus.CONFLICT;
            // Una excepción del dominio que aún no está en la tabla de DC-15 es una regla incumplida por el cliente.
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
