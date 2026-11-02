package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.ExcepcionDominio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de los indicadores a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, como en el
 * catálogo (DC-15). Hoy solo llega DATO_INVALIDO: un periodo o una exposición que no sirven.
 */
@RestControllerAdvice(basePackageClasses = IndicadoresController.class)
public class ManejadorErroresIndicadores {

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excepcion.getMessage());
        problema.setProperty("codigo", excepcion.codigo());
        return problema;
    }
}
