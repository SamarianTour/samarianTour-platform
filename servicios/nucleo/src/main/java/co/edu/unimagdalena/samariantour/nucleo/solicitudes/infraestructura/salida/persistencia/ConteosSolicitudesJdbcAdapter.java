package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CategoriaPrestador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoEstado;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ConteosSolicitudesPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Los conteos de los indicadores con GROUP BY sobre {@code solicitudes} y {@code transiciones_solicitud} (V3, V14).
 * Solo leen, así que basta con JDBC; el índice {@code solicitudes_por_creacion} filtra el periodo.
 */
@Component
public class ConteosSolicitudesJdbcAdapter implements ConteosSolicitudesPort {

    private final JdbcTemplate jdbc;

    ConteosSolicitudesJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ConteoDemanda> porZonaYTipo(Instant desde, Instant hasta) {
        return jdbc.query("""
                        SELECT zona_id, tipo_oferta, COUNT(*) AS solicitudes FROM solicitudes
                        WHERE creada_en >= ? AND creada_en < ?
                        GROUP BY zona_id, tipo_oferta""",
                (fila, n) -> new ConteoDemanda(fila.getString("zona_id"),
                        TipoOferta.valueOf(fila.getString("tipo_oferta")), fila.getLong("solicitudes")),
                Timestamp.from(desde), Timestamp.from(hasta));
    }

    /** Una solicitud cuenta una vez por estado aunque haya pasado dos veces por él, como PAGO_FALLIDO. */
    @Override
    public List<ConteoEstado> porEstadoAlcanzado(Instant desde, Instant hasta) {
        return jdbc.query("""
                        SELECT s.tipo_oferta, t.estado_destino, COUNT(DISTINCT s.id) AS solicitudes
                        FROM solicitudes s JOIN transiciones_solicitud t ON t.solicitud_id = s.id
                        WHERE s.creada_en >= ? AND s.creada_en < ?
                        GROUP BY s.tipo_oferta, t.estado_destino""",
                (fila, n) -> new ConteoEstado(TipoOferta.valueOf(fila.getString("tipo_oferta")),
                        EstadoSolicitud.valueOf(fila.getString("estado_destino")), fila.getLong("solicitudes")),
                Timestamp.from(desde), Timestamp.from(hasta));
    }

    @Override
    public List<ConteoCategoria> porCategoria(Instant desde, Instant hasta) {
        return jdbc.query("""
                        SELECT categoria_prestador, COUNT(*) AS solicitudes,
                               COUNT(*) FILTER (WHERE estado = 'PAGADA') AS pagadas
                        FROM solicitudes
                        WHERE creada_en >= ? AND creada_en < ?
                        GROUP BY categoria_prestador""",
                (fila, n) -> {
                    String categoria = fila.getString("categoria_prestador");
                    return new ConteoCategoria(categoria == null ? null : CategoriaPrestador.valueOf(categoria),
                            fila.getLong("solicitudes"), fila.getLong("pagadas"));
                },
                Timestamp.from(desde), Timestamp.from(hasta));
    }
}
