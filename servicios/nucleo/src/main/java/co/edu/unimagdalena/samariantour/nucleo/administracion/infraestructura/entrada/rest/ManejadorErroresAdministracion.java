package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaInternaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaPropiaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ModeracionYaDecididaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorConPrestadoresException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorNoValidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.PrestadorNoHabilitadoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.VersionNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.AvisoNoReenviableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.EventoNoReintentableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de la administración a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, como en
 * el catálogo (DC-15). Solo atiende a los controladores de este módulo.
 */
@RestControllerAdvice(basePackageClasses = ModeracionAdminController.class)
public class ManejadorErroresAdministracion {

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estadoDe(excepcion), excepcion.getMessage());
        problema.setProperty("codigo", excepcion.codigo());
        return problema;
    }

    /** Un aviso de la bandeja que no existe (404) o no está rechazado (409) (DN-44). */
    @ExceptionHandler(AvisoNoReenviableException.class)
    ProblemDetail avisoNoReenviable(AvisoNoReenviableException excepcion) {
        return problema(excepcion.codigo().endsWith("NO_ENCONTRADO"), excepcion.codigo(), excepcion.getMessage());
    }

    /** Un evento de pago que no existe (404) o no está fallido (409) (DN-44). */
    @ExceptionHandler(EventoNoReintentableException.class)
    ProblemDetail eventoNoReintentable(EventoNoReintentableException excepcion) {
        return problema(excepcion.codigo().endsWith("NO_ENCONTRADO"), excepcion.codigo(), excepcion.getMessage());
    }

    private static ProblemDetail problema(boolean noEncontrado, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                noEncontrado ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT, detalle);
        problema.setProperty("codigo", codigo);
        return problema;
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case NoEstaEnModeracionException e -> HttpStatus.NOT_FOUND;
            case VersionNoVigenteException e -> HttpStatus.CONFLICT;
            case ModeracionYaDecididaException e -> HttpStatus.CONFLICT;
            case PrestadorNoHabilitadoException e -> HttpStatus.CONFLICT;
            case CatalogoNoDisponibleException e -> HttpStatus.SERVICE_UNAVAILABLE;
            case PrestadorNoRegistradoException e -> HttpStatus.NOT_FOUND;
            case CuentaInternaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case OperadorNoValidoException e -> HttpStatus.CONFLICT;
            case OperadorConPrestadoresException e -> HttpStatus.CONFLICT;
            case CuentaPropiaException e -> HttpStatus.CONFLICT;
            case CorreoYaRegistradoException e -> HttpStatus.CONFLICT;
            case CorreoNoDisponibleException e -> HttpStatus.SERVICE_UNAVAILABLE;

            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
