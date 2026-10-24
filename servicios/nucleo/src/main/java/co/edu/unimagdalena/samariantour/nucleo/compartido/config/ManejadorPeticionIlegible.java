package co.edu.unimagdalena.samariantour.nucleo.compartido.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Una petición que no se puede leer responde 400 {@code DATO_INVALIDO}, como en el catálogo (DC-15): JSON mal formado
 * o un parámetro que no es del tipo esperado. Atiende a todos los módulos del servicio.
 */
@RestControllerAdvice
// Antes que el manejador por defecto de Spring, para que una petición ilegible también lleve el código.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorPeticionIlegible {

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail peticionIlegible(Exception excepcion) {
        String detalle = excepcion instanceof MethodArgumentTypeMismatchException parametro
                ? "%s: el valor no tiene el formato esperado".formatted(parametro.getName())
                : "El cuerpo de la petición no se pudo leer: revisa el JSON y el tipo de cada campo";
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
        problema.setProperty("codigo", "DATO_INVALIDO");
        return problema;
    }
}
