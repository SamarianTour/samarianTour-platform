package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.AlertasNoDisponiblesException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, igual que el
 * catálogo: 400 DATO_INVALIDO, 404 OFERTA_NO_ENCONTRADA y 503 RECOMENDACION_NO_DISPONIBLE o ALERTAS_NO_DISPONIBLES.
 * Una falla del proveedor de IA nunca llega aquí: la resuelve el motor por reglas.
 */
@RestControllerAdvice
// Antes que el manejador por defecto de Spring, para que una petición ilegible también lleve el código.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorErrores {

    private static final String CODIGO = "codigo";

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        return problema(estadoDe(excepcion), excepcion.codigo(), excepcion.getMessage());
    }

    /** JSON mal formado o un valor que no es del tipo esperado, por ejemplo un tipo de oferta que no existe. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail peticionIlegible(HttpMessageNotReadableException excepcion) {
        return problema(HttpStatus.BAD_REQUEST, "DATO_INVALIDO",
                "El cuerpo de la petición no se pudo leer: revisa el JSON y el tipo de cada campo");
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case OfertaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case RecomendacionNoDisponibleException e -> HttpStatus.SERVICE_UNAVAILABLE;
            case AlertasNoDisponiblesException e -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ProblemDetail problema(HttpStatus estado, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setProperty(CODIGO, codigo);
        return problema;
    }
}
