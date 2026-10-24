package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Almacenamiento de la bandeja de salida (DN-06). */
public interface BandejaSalidaRepositoryPort {

    /** @return false si ya había un aviso con ese identificador, que no se toca */
    boolean agregar(AvisoSaliente aviso);

    /**
     * Toma para esta transacción hasta {@code limite} avisos pendientes cuyo intento ya toca, del que toca antes al
     * que toca después. Los que otra transacción tiene tomados se saltan: otra réplica los está enviando.
     */
    List<AvisoSaliente> tomarPendientes(Instant ahora, int limite);

    void guardar(AvisoSaliente aviso);

    Optional<AvisoSaliente> buscar(String idAviso);
}
