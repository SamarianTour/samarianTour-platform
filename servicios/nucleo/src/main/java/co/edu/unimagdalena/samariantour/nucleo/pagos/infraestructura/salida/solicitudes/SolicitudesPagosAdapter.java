package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.PagoNoAplicableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EfectoEnSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SolicitudCobrable;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.SolicitudesPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.EventoPagoNoAplicableException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.ResultadoPago;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.SolicitudesPagosApi;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Las solicitudes por {@code solicitudes.api} (DN-17): nunca su dominio ni sus tablas. Traduce los tipos y las
 * excepciones de la API a los de pagos. Se llama dentro de la transacción de pagos.
 */
@Component
public class SolicitudesPagosAdapter implements SolicitudesPort {

    private final SolicitudesPagosApi solicitudes;

    public SolicitudesPagosAdapter(SolicitudesPagosApi solicitudes) {
        this.solicitudes = solicitudes;
    }

    @Override
    public Optional<SolicitudCobrable> consultar(UUID solicitudId, UUID turistaId) {
        return solicitudes.consultarParaCobro(solicitudId, turistaId)
                .map(solicitud -> new SolicitudCobrable(solicitud.solicitudId(), solicitud.estado(),
                        solicitud.precioBase(), solicitud.venceEn(), solicitud.prestadorId(), solicitud.ofertaId()));
    }

    @Override
    public EfectoEnSolicitud marcarIntentoEnCurso(UUID solicitudId) {
        return traducir(() -> solicitudes.marcarIntentoEnCurso(solicitudId));
    }

    @Override
    public EfectoEnSolicitud liberarIntentoEnCurso(UUID solicitudId) {
        return traducir(() -> solicitudes.liberarIntentoEnCurso(solicitudId));
    }

    @Override
    public EfectoEnSolicitud registrarPagoAprobado(UUID solicitudId) {
        return traducir(() -> solicitudes.registrarPagoAprobado(solicitudId));
    }

    @Override
    public EfectoEnSolicitud registrarPagoRechazado(UUID solicitudId) {
        return traducir(() -> solicitudes.registrarPagoRechazado(solicitudId));
    }

    private static EfectoEnSolicitud traducir(Supplier<ResultadoPago> evento) {
        try {
            return EfectoEnSolicitud.valueOf(evento.get().name());
        } catch (EventoPagoNoAplicableException e) {
            throw new PagoNoAplicableException(e.codigo(), e.getMessage());
        }
    }
}
