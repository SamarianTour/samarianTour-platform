package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.FotoNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEditableException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PerfilNoEncontradoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Traduce las excepciones del dominio a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, estable para que
 * los clientes expliquen el motivo (DC-15). Los demás errores de Spring MVC los responde su manejador por defecto,
 * también como ProblemDetail.
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

    /** JSON mal formado o un parámetro que no es del tipo esperado, por ejemplo un precio que no es un número. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail peticionIlegible(Exception excepcion) {
        String detalle = excepcion instanceof MethodArgumentTypeMismatchException parametro
                ? "%s: el valor no tiene el formato esperado".formatted(parametro.getName())
                : "El cuerpo de la petición no se pudo leer: revisa el JSON y el tipo de cada campo";
        return problema(HttpStatus.BAD_REQUEST, "DATO_INVALIDO", detalle);
    }

    /**
     * Una subida de foto rechazada antes de llegar al controlador: el archivo pasa del máximo, falta la parte
     * {@code foto} o la petición no es multipart. Es la misma regla que valida el caso de uso (DC-39).
     */
    @ExceptionHandler({MaxUploadSizeExceededException.class, MissingServletRequestPartException.class,
            MultipartException.class})
    ProblemDetail subidaInvalida(Exception excepcion) {
        String detalle = excepcion instanceof MaxUploadSizeExceededException
                ? "foto: pesa más de 5 MB"
                : "foto: envía la imagen como multipart/form-data en la parte 'foto'";
        return problema(HttpStatus.BAD_REQUEST, "DATO_INVALIDO", detalle);
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case ZonaNoDisponibleException e -> HttpStatus.BAD_REQUEST;
            case PrestadorNoAutorizadoException e -> HttpStatus.FORBIDDEN;
            case TipoOfertaNoPermitidoException e -> HttpStatus.FORBIDDEN;
            case RntInactivoException e -> HttpStatus.FORBIDDEN;
            case OfertaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case PerfilNoEncontradoException e -> HttpStatus.NOT_FOUND;
            case FotoNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case ZonaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case OfertaNoEditableException e -> HttpStatus.CONFLICT;
            case ZonaDuplicadaException e -> HttpStatus.CONFLICT;
            // Una excepción del dominio que aún no está en la tabla de DC-15 es una regla incumplida por el cliente.
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ProblemDetail problema(HttpStatus estado, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setProperty(CODIGO, codigo);
        return problema;
    }
}
