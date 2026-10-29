package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CredencialesInvalidasException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaDesactivadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.PoliticaNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.SesionInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de las cuentas a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, como en el
 * catálogo (DC-15). Solo atiende a los controladores de este módulo.
 */
@RestControllerAdvice(basePackageClasses = UsuarioController.class)
public class ManejadorErroresCuentas {

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estadoDe(excepcion), excepcion.getMessage());
        problema.setProperty("codigo", excepcion.codigo());
        if (excepcion instanceof CodigoIncorrectoException incorrecto) {
            problema.setProperty("intentosRestantes", incorrecto.intentosRestantes());
        }
        if (excepcion instanceof CuentaBloqueadaException bloqueada) {
            problema.setProperty("bloqueadaHasta", bloqueada.bloqueadaHasta());
        }
        return problema;
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case CodigoIncorrectoException e -> HttpStatus.BAD_REQUEST;
            case CredencialesInvalidasException e -> HttpStatus.UNAUTHORIZED;
            case SesionInvalidaException e -> HttpStatus.UNAUTHORIZED;
            case ContrasenaIncorrectaException e -> HttpStatus.UNAUTHORIZED;
            case CuentaDesactivadaException e -> HttpStatus.FORBIDDEN;
            case CuentaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case CuentaBloqueadaException e -> HttpStatus.LOCKED;
            case CorreoYaRegistradoException e -> HttpStatus.CONFLICT;
            case PoliticaNoVigenteException e -> HttpStatus.CONFLICT;
            case CodigoNoVigenteException e -> HttpStatus.CONFLICT;
            case CorreoNoDisponibleException e -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
