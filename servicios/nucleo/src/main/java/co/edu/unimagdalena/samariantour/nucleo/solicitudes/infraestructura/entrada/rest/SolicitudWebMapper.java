package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.AvisoPlataforma;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CondicionesOfrecidas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DetalleSolicitudOperador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.AvisoResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.CondicionesResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.OfertaSolicitadaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudColaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudOperadorResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudResumenResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudTuristaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.TransicionResponse;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/** Convierte las solicitudes del dominio en las respuestas de la API: la del turista y la del operador. */
final class SolicitudWebMapper {

    private SolicitudWebMapper() {
    }

    static SolicitudTuristaResponse aTurista(SolicitudDisponibilidad solicitud) {
        return new SolicitudTuristaResponse(solicitud.id(), oferta(solicitud), solicitud.fechaInicio(),
                solicitud.horaSalida(), solicitud.horaReserva(), solicitud.fechaSalida(), solicitud.numeroUnidades(),
                solicitud.personas(), solicitud.notaTurista(), solicitud.estado(),
                condiciones(solicitud), solicitud.venceEn(), solicitud.creadaEn(),
                solicitud.actualizadaEn(), historial(solicitud));
    }

    static SolicitudResumenResponse aResumen(SolicitudDisponibilidad solicitud) {
        return new SolicitudResumenResponse(solicitud.id(), oferta(solicitud), solicitud.fechaInicio(),
                solicitud.horaSalida(), solicitud.horaReserva(), solicitud.fechaSalida(), solicitud.numeroUnidades(),
                solicitud.personas(), solicitud.estado(), solicitud.venceEn(), solicitud.creadaEn(),
                solicitud.actualizadaEn());
    }

    static SolicitudColaResponse aCola(SolicitudDisponibilidad solicitud, Instant ahora) {
        return new SolicitudColaResponse(solicitud.id(), oferta(solicitud), solicitud.fechaInicio(),
                solicitud.horaSalida(), solicitud.horaReserva(), solicitud.fechaSalida(), solicitud.numeroUnidades(),
                solicitud.personas(), solicitud.estado(), solicitud.venceEn(), solicitud.proximaAVencer(ahora),
                solicitud.creadaEn());
    }

    static SolicitudOperadorResponse aOperador(DetalleSolicitudOperador detalle, Instant ahora) {
        SolicitudDisponibilidad solicitud = detalle.solicitud();
        return new SolicitudOperadorResponse(solicitud.id(), oferta(solicitud), solicitud.oferta().prestadorId(),
                detalle.whatsappPrestador(), solicitud.fechaInicio(), solicitud.horaSalida(), solicitud.horaReserva(),
                solicitud.fechaSalida(), solicitud.numeroUnidades(), solicitud.personas(), solicitud.notaTurista(),
                solicitud.estado(), condiciones(solicitud), solicitud.venceEn(),
                solicitud.proximaAVencer(ahora), solicitud.creadaEn(), solicitud.actualizadaEn(), historial(solicitud));
    }

    static AvisoResponse aAviso(AvisoPlataforma aviso) {
        return new AvisoResponse(aviso.id(), aviso.tipo(), aviso.solicitudId(), aviso.datos(), aviso.creadoEn());
    }

    static <T, R> PaginaResponse<R> pagina(Pagina<T> pagina, Function<T, R> conversion) {
        List<R> contenido = pagina.contenido().stream().map(conversion).toList();
        return new PaginaResponse<>(contenido, pagina.pagina(), pagina.tamano(), pagina.totalElementos(),
                pagina.totalPaginas());
    }

    private static OfertaSolicitadaResponse oferta(SolicitudDisponibilidad solicitud) {
        return new OfertaSolicitadaResponse(solicitud.ofertaId(), solicitud.oferta().tipo(),
                solicitud.oferta().nombre(), solicitud.oferta().zonaId());
    }

    private static CondicionesResponse condiciones(SolicitudDisponibilidad solicitud) {
        CondicionesOfrecidas condiciones = solicitud.condiciones();
        return condiciones == null ? null : new CondicionesResponse(condiciones.precioBase(),
                condiciones.politicaCancelacion(), condiciones.nota(), solicitud.aparteHasta());
    }

    private static List<TransicionResponse> historial(SolicitudDisponibilidad solicitud) {
        return solicitud.historial().stream()
                .map(transicion -> new TransicionResponse(transicion.de(), transicion.a(), transicion.en(),
                        transicion.actor(), transicion.motivo()))
                .toList();
    }
}
