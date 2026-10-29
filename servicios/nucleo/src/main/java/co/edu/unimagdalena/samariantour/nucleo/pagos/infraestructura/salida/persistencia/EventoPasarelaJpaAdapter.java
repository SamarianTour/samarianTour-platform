package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoTransaccion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Los eventos del webhook en {@code eventos_pasarela} (DN-21): únicos por su clave, tomados con SKIP LOCKED. */
@Component
public class EventoPasarelaJpaAdapter implements EventoPasarelaRepositoryPort {

    private final EventoPasarelaJpaRepository eventos;

    EventoPasarelaJpaAdapter(EventoPasarelaJpaRepository eventos) {
        this.eventos = eventos;
    }

    @Override
    public boolean guardarSiNuevo(EventoPasarela evento) {
        return eventos.guardarSiNuevo(evento.id(), evento.claveUnica(), evento.transaccionId(), evento.referencia(),
                evento.estado().name(), evento.montoCentavos(), evento.cuerpo(), evento.recibidoEn()) == 1;
    }

    @Override
    public Optional<EventoPasarela> tomarSinProcesar() {
        return eventos.tomarSinProcesar().map(fila -> new EventoPasarela(fila.id(), fila.claveUnica(),
                fila.transaccionId(), fila.referencia(), EstadoTransaccion.valueOf(fila.estado()),
                fila.montoCentavos(), fila.cuerpo(), fila.recibidoEn(), fila.procesadoEn(), fila.resultado()));
    }

    @Override
    public void marcarProcesado(EventoPasarela evento) {
        eventos.marcarProcesado(evento.id(), evento.procesadoEn(), evento.resultado());
    }
}
