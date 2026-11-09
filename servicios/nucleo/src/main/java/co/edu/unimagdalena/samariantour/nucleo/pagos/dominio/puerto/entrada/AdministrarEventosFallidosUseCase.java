package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;

import java.util.List;
import java.util.UUID;

/** Los eventos que fallaron cinco veces al procesarse, para revisarlos y devolverlos a la cola (DN-21, DN-44). */
public interface AdministrarEventosFallidosUseCase {

    /** Del recibido más reciente al más antiguo. */
    List<EventoPasarela> fallidos();

    /**
     * Lo devuelve a la cola, con los fallos en cero: la tarea de eventos lo procesa en los siguientes 5 s.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoReintentableException si no
     *         existe o no está fallido
     */
    EventoPasarela reintentar(UUID eventoId);
}
