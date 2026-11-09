package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

import java.util.List;
import java.util.UUID;

/**
 * Lo que otro módulo todavía tiene pendiente con un prestador, que le impide suprimir sus datos (RF-13, DN-45). La
 * implementan solicitudes (las pagadas cuyo servicio no ha pasado y los pagos en curso) y pagos (las dispersiones por
 * hacerle). Prestadores no puede depender de ellos, que ya dependen de él: por eso declara aquí lo que necesita, como
 * {@link ObservadorCuentaCobro}.
 */
public interface CompromisosPrestador {

    /** Una frase por cada clase de compromiso, por ejemplo "2 solicitudes pagadas por prestar"; vacía si no hay. */
    List<String> pendientes(UUID prestadorId);
}
