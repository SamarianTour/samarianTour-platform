package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.ContrasenaRechazadaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaCobroNoRegistradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorConCompromisosException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.TipoNoCoincideConRntException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.WhatsappNoConfiguradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de los prestadores a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, como en el
 * catálogo (DC-15). Solo atiende a los controladores de este módulo.
 */
@RestControllerAdvice(basePackageClasses = PrestadorController.class)
public class ManejadorErroresPrestadores {

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estadoDe(excepcion), excepcion.getMessage());
        problema.setProperty("codigo", excepcion.codigo());
        if (excepcion instanceof CodigoIncorrectoException incorrecto) {
            problema.setProperty("intentosRestantes", incorrecto.intentosRestantes());
        }
        if (excepcion instanceof ContrasenaRechazadaException rechazada && rechazada.bloqueadaHasta() != null) {
            problema.setProperty("bloqueadaHasta", rechazada.bloqueadaHasta());
        }
        if (excepcion instanceof PrestadorConCompromisosException conCompromisos) {
            problema.setProperty("compromisos", conCompromisos.compromisos());
        }
        return problema;
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case CodigoIncorrectoException e -> HttpStatus.BAD_REQUEST;
            case CodigoNoVigenteException e -> HttpStatus.CONFLICT;
            case WhatsappNoConfiguradoException e -> HttpStatus.CONFLICT;
            case CuentaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case PrestadorNoRegistradoException e -> HttpStatus.NOT_FOUND;
            case CuentaCobroNoRegistradaException e -> HttpStatus.NOT_FOUND;
            case PrestadorYaRegistradoException e -> HttpStatus.CONFLICT;
            case RntYaRegistradoException e -> HttpStatus.CONFLICT;
            case TipoNoCoincideConRntException e -> HttpStatus.CONFLICT;
            case PrestadorConCompromisosException e -> HttpStatus.CONFLICT;
            case ContrasenaRechazadaException e -> switch (e.codigo()) {
                case "CUENTA_BLOQUEADA" -> HttpStatus.LOCKED;
                case "CUENTA_NO_ENCONTRADA" -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.UNAUTHORIZED;
            };
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
