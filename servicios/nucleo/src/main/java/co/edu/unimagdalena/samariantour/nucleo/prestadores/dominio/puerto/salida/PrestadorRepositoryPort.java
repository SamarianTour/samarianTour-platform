package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Los prestadores, con el WhatsApp cifrado (RNF-08). */
public interface PrestadorRepositoryPort {

    /** @throws RntYaRegistradoException si otro prestador ya tiene el número de RNT */
    void insertar(Prestador prestador);

    /**
     * Con bloqueo optimista (ADR-05).
     *
     * @throws RntYaRegistradoException si el número de RNT nuevo ya es de otro prestador
     */
    void actualizar(Prestador prestador);

    Optional<Prestador> porId(UUID id);

    /**
     * Toma hasta {@code limite} prestadores que la sincronización que empezó en {@code inicio} todavía no tomó, y los
     * marca como tomados (RF-76, DN-36). Dos réplicas nunca toman el mismo. Va dentro de una transacción.
     */
    List<UUID> tomarParaSincronizar(Instant inicio, int limite);
}
