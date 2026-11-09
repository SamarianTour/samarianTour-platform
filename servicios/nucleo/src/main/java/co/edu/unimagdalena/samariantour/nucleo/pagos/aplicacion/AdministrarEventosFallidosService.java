package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoReintentableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoProcesamiento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.AdministrarEventosFallidosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Lista los eventos fallidos y devuelve uno a la cola (DN-44). El evento se lee bloqueado, así dos administradores no
 * lo reintentan a la vez ni se cruzan con la tarea que avisa de los fallidos.
 */
public class AdministrarEventosFallidosService implements AdministrarEventosFallidosUseCase {

    private final EventoPasarelaRepositoryPort eventos;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public AdministrarEventosFallidosService(EventoPasarelaRepositoryPort eventos, TransaccionPort transaccion,
                                             Clock reloj) {
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public List<EventoPasarela> fallidos() {
        return eventos.fallidos();
    }

    @Override
    public EventoPasarela reintentar(UUID eventoId) {
        return transaccion.ejecutar(() -> {
            EventoPasarela evento = eventos.porIdParaCambiar(eventoId).orElseThrow(() ->
                    new EventoNoReintentableException(EventoNoReintentableException.NO_ENCONTRADO, eventoId));
            if (evento.procesamiento() != EstadoProcesamiento.FALLIDO) {
                throw new EventoNoReintentableException(EventoNoReintentableException.NO_FALLIDO, eventoId);
            }
            EventoPasarela reintentado = evento.reintentado(reloj.instant());
            eventos.reintentar(reintentado);
            return reintentado;
        });
    }
}
