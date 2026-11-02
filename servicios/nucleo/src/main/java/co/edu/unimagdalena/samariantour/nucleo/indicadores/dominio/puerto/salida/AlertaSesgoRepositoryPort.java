package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AlertaSesgo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Las alertas de sesgo y las revisiones de cada día (DN-43). Es el {@code AvisoPlataformaPort} del diagrama de
 * paquetes: el aviso queda en la base y el administrador lo ve en su panel.
 */
public interface AlertaSesgoRepositoryPort {

    /** De la de mayor índice a la de menor. */
    List<AlertaSesgo> abiertas();

    /** La agrega o guarda sus cambios. */
    void guardar(AlertaSesgo alerta);

    boolean revisado(LocalDate dia);

    /**
     * Reserva la revisión del día. Va dentro de la transacción de la revisión: si la revisión falla, la reserva se
     * deshace.
     *
     * @return si la reservó; falso si otra réplica ya lo hizo
     */
    boolean reservarRevision(LocalDate dia, Instant ahora);
}
