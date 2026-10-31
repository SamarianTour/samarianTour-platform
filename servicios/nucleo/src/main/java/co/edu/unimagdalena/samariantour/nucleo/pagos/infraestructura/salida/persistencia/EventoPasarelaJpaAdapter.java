package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoProcesamiento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoTransaccion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<EventoPasarela> tomarSinProcesar(Instant ahora) {
        return eventos.tomarSinProcesar(ahora).map(EventoPasarelaJpaAdapter::aDominio);
    }

    @Override
    public void marcarProcesado(EventoPasarela evento) {
        eventos.marcarProcesado(evento.id(), evento.procesadoEn(), evento.resultado());
    }

    @Override
    public Optional<EventoPasarela> pendientePorId(UUID id) {
        return eventos.pendientePorId(id).map(EventoPasarelaJpaAdapter::aDominio);
    }

    @Override
    public void registrarFallo(EventoPasarela evento) {
        eventos.registrarFallo(evento.id(), evento.procesamiento().name(), evento.intentosProcesamiento(),
                evento.ultimoError(), evento.siguienteIntentoEn());
    }

    @Override
    public Optional<EventoPasarela> tomarFallidoSinAviso(Instant inicio) {
        return eventos.tomarFallidoSinAviso(inicio).map(EventoPasarelaJpaAdapter::aDominio);
    }

    @Override
    public void marcarAvisado(EventoPasarela evento) {
        eventos.marcarAvisado(evento.id(), evento.avisoEnviadoEn());
    }

    @Override
    public void marcarAvisoIntentado(UUID id, Instant ahora) {
        eventos.marcarAvisoIntentado(id, ahora);
    }

    private static EventoPasarela aDominio(EventoPasarelaEntity fila) {
        return new EventoPasarela(fila.id(), fila.claveUnica(), fila.transaccionId(), fila.referencia(),
                EstadoTransaccion.valueOf(fila.estado()), fila.montoCentavos(), fila.cuerpo(), fila.recibidoEn(),
                fila.procesadoEn(), fila.resultado(), EstadoProcesamiento.valueOf(fila.procesamiento()),
                fila.intentosProcesamiento(), fila.ultimoError(), fila.siguienteIntentoEn(), fila.avisoEnviadoEn());
    }
}
