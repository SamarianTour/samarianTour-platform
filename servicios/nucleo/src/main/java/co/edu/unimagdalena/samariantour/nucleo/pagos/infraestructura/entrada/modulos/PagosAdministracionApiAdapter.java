package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.pagos.api.ComisionConfigurada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.ComisionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.EventoNoReintentableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.EventoPagoFallido;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.PagoTardioRegistrado;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.PagosAdministracionApi;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ActualizarComisionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.AdministrarEventosFallidosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ConsultarPagosTardiosUseCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Atiende al módulo de administración por {@code pagos.api}, como un controlador pero dentro del proceso (DN-23):
 * llama a los casos de uso y traduce sus tipos a los de la API.
 */
@Component
public class PagosAdministracionApiAdapter implements PagosAdministracionApi {

    private final ActualizarComisionUseCase comision;
    private final ConsultarPagosTardiosUseCase tardios;
    private final AdministrarEventosFallidosUseCase eventos;

    public PagosAdministracionApiAdapter(ActualizarComisionUseCase comision, ConsultarPagosTardiosUseCase tardios,
                                         AdministrarEventosFallidosUseCase eventos) {
        this.comision = comision;
        this.tardios = tardios;
        this.eventos = eventos;
    }

    @Override
    public List<ComisionConfigurada> historialComision() {
        return comision.historial().stream().map(PagosAdministracionApiAdapter::aApi).toList();
    }

    @Override
    public ComisionConfigurada actualizarComision(BigDecimal porcentaje, UUID administradorId) {
        try {
            return aApi(comision.actualizar(porcentaje, administradorId));
        } catch (DatoInvalidoException e) {
            throw new ComisionInvalidaException(e.getMessage());
        }
    }

    @Override
    public List<PagoTardioRegistrado> pagosTardios() {
        return tardios.consultar().stream()
                .map(pago -> new PagoTardioRegistrado(pago.id(), pago.solicitudId(), pago.referencia(),
                        pago.transaccionId(), pago.montoCentavos() / 100, pago.motivo().name(), pago.registradoEn(),
                        pago.avisoEnviadoEn() != null))
                .toList();
    }

    @Override
    public List<EventoPagoFallido> eventosFallidos() {
        return eventos.fallidos().stream().map(PagosAdministracionApiAdapter::aApi).toList();
    }

    @Override
    public EventoPagoFallido reintentarEvento(UUID eventoId) {
        try {
            return aApi(eventos.reintentar(eventoId));
        } catch (co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoReintentableException e) {
            throw new EventoNoReintentableException(e.codigo(), e.getMessage());
        }
    }

    private static EventoPagoFallido aApi(EventoPasarela evento) {
        return new EventoPagoFallido(evento.id(), evento.referencia(), evento.transaccionId(), evento.estado().name(),
                evento.montoCentavos() / 100, evento.intentosProcesamiento(), evento.ultimoError(),
                evento.recibidoEn());
    }

    private static ComisionConfigurada aApi(ConfiguracionComision configuracion) {
        return new ComisionConfigurada(configuracion.porcentaje(), configuracion.vigenteDesde(),
                configuracion.administradorId());
    }
}
