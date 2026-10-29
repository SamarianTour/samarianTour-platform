package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.ClaveIdempotenciaReutilizadaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoAutenticoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.SolicitudNoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de pagos a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, como el resto del
 * núcleo. Solo atiende a los controladores de este módulo:
 * <ul>
 *   <li>400 DATO_INVALIDO; 401 EVENTO_NO_AUTENTICO (el webhook); 404 SOLICITUD_NO_ENCONTRADA;</li>
 *   <li>409 SOLICITUD_NO_COBRABLE, INTENTOS_AGOTADOS y los de la solicitud, como PLAZO_VENCIDO;</li>
 *   <li>422 CLAVE_IDEMPOTENCIA_REUTILIZADA.</li>
 * </ul>
 * Un evento rechazado se registra sin su cuerpo, que puede traer datos del pago.
 */
@RestControllerAdvice(basePackageClasses = PagoController.class)
public class ManejadorErroresPagos {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresPagos.class);

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        if (excepcion instanceof EventoNoAutenticoException) {
            LOG.warn("webhook de pagos: evento rechazado: {}", excepcion.getMessage());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estadoDe(excepcion), excepcion.getMessage());
        problema.setProperty("codigo", excepcion.codigo());
        return problema;
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case EventoNoAutenticoException e -> HttpStatus.UNAUTHORIZED;
            case SolicitudNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case ClaveIdempotenciaReutilizadaException e -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.CONFLICT;
        };
    }
}
