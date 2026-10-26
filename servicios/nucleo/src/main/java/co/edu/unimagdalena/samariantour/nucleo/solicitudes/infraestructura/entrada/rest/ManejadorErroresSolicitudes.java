package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ClaveIdempotenciaReutilizadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.PrestadorSinOperadorException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de las solicitudes a ProblemDetail (RFC 7807) con la propiedad {@code codigo}, como el resto
 * del núcleo. Solo atiende a los controladores de este módulo:
 * <ul>
 *   <li>400 DATO_INVALIDO; 404 SOLICITUD_NO_ENCONTRADA y OFERTA_NO_ENCONTRADA;</li>
 *   <li>409 los conflictos con el estado: TRANSICION_INVALIDA, PLAZO_VENCIDO, SIN_TIEMPO_PARA_CONFIRMAR,
 *   SOLICITUD_DUPLICADA, PRESTADOR_SIN_OPERADOR y WHATSAPP_NO_VERIFICADO;</li>
 *   <li>422 CLAVE_IDEMPOTENCIA_REUTILIZADA; 503 CATALOGO_NO_DISPONIBLE.</li>
 * </ul>
 * Un prestador sin operador deja además la incidencia en el log, con el prefijo {@code [INCIDENCIA RF-32]}, para que
 * el administrador le asigne uno (RF-32, RF-81).
 */
@RestControllerAdvice(basePackageClasses = SolicitudTuristaController.class)
public class ManejadorErroresSolicitudes {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresSolicitudes.class);

    @ExceptionHandler(ExcepcionDominio.class)
    ProblemDetail excepcionDominio(ExcepcionDominio excepcion) {
        if (excepcion instanceof PrestadorSinOperadorException sinOperador) {
            LOG.warn("[INCIDENCIA RF-32] solicitud rechazada: el prestador {} de la oferta {} no tiene operador de "
                    + "disponibilidad asignado", sinOperador.prestadorId(), sinOperador.ofertaId());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estadoDe(excepcion), excepcion.getMessage());
        problema.setProperty("codigo", excepcion.codigo());
        return problema;
    }

    private static HttpStatus estadoDe(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case DatoInvalidoException e -> HttpStatus.BAD_REQUEST;
            case SolicitudNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case OfertaNoEncontradaException e -> HttpStatus.NOT_FOUND;
            case ClaveIdempotenciaReutilizadaException e -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CatalogoNoDisponibleException e -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.CONFLICT;
        };
    }
}
