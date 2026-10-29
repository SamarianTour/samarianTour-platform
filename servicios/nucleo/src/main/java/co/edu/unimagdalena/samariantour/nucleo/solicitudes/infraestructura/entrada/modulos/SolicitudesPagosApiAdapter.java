package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.EventoPagoNoAplicableException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.ResultadoPago;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.SolicitudParaCobro;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.SolicitudesPagosApi;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoEventoPago;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.RegistrarEventoPagoUseCase;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Atiende al módulo de pagos por {@code solicitudes.api}, como un controlador pero dentro del proceso (DN-17): llama al
 * caso de uso y traduce sus tipos y excepciones a los de la API, para que pagos no dependa del dominio de solicitudes.
 */
@Component
public class SolicitudesPagosApiAdapter implements SolicitudesPagosApi {

    private final RegistrarEventoPagoUseCase eventos;

    public SolicitudesPagosApiAdapter(RegistrarEventoPagoUseCase eventos) {
        this.eventos = eventos;
    }

    @Override
    public Optional<SolicitudParaCobro> consultarParaCobro(UUID solicitudId, UUID turistaId) {
        return eventos.consultarParaCobro(solicitudId, turistaId)
                .map(datos -> new SolicitudParaCobro(datos.solicitudId(), datos.estado().name(), datos.precioBase(),
                        datos.venceEn(), datos.prestadorId(), datos.ofertaId()));
    }

    @Override
    public ResultadoPago marcarIntentoEnCurso(UUID solicitudId) {
        return traducir(() -> eventos.marcarIntentoEnCurso(solicitudId));
    }

    @Override
    public ResultadoPago liberarIntentoEnCurso(UUID solicitudId) {
        return traducir(() -> eventos.liberarIntentoEnCurso(solicitudId));
    }

    @Override
    public ResultadoPago registrarPagoAprobado(UUID solicitudId) {
        return traducir(() -> eventos.registrarPagoAprobado(solicitudId));
    }

    @Override
    public ResultadoPago registrarPagoRechazado(UUID solicitudId) {
        return traducir(() -> eventos.registrarPagoRechazado(solicitudId));
    }

    private static ResultadoPago traducir(Supplier<ResultadoEventoPago> evento) {
        try {
            return ResultadoPago.valueOf(evento.get().name());
        } catch (ExcepcionDominio e) {
            throw new EventoPagoNoAplicableException(e.codigo(), e.getMessage());
        }
    }
}
