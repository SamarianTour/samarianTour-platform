package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AparicionesPrestador;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertaExpuesta;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.ExposicionRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** El registro de exposición en {@code exposiciones} (V15). Solo se insertan y se cuentan filas: basta con JDBC. */
@Component
public class ExposicionJdbcAdapter implements ExposicionRepositoryPort {

    private final JdbcTemplate jdbc;

    ExposicionJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(RegistroExposicion registro) {
        Timestamp fecha = Timestamp.from(registro.fecha());
        List<Object[]> filas = registro.ofertas().stream()
                .map((OfertaExpuesta oferta) -> new Object[] {UUID.randomUUID(), registro.id(), fecha,
                        registro.origen().name(), registro.motor(), oferta.ofertaId(), oferta.prestadorId(),
                        oferta.posicion()})
                .toList();
        jdbc.batchUpdate("""
                INSERT INTO exposiciones (id, consulta_id, registrada_en, origen, motor, oferta_id, prestador_id,
                                          posicion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", filas);
    }

    @Override
    public long recomendacionesDesde(Instant desde) {
        Long recomendaciones = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT consulta_id) FROM exposiciones
                WHERE origen = 'RECOMENDACION' AND registrada_en >= ?""", Long.class, Timestamp.from(desde));
        return recomendaciones == null ? 0 : recomendaciones;
    }

    @Override
    public List<AparicionesPrestador> aparicionesDesde(Instant desde) {
        return jdbc.query("""
                        SELECT prestador_id, COUNT(*) AS apariciones FROM exposiciones
                        WHERE origen = 'RECOMENDACION' AND registrada_en >= ?
                        GROUP BY prestador_id""",
                (fila, n) -> new AparicionesPrestador(fila.getObject("prestador_id", UUID.class),
                        fila.getLong("apariciones")),
                Timestamp.from(desde));
    }

    @Override
    public int borrarAnteriores(Instant limite) {
        return jdbc.update("DELETE FROM exposiciones WHERE registrada_en < ?", Timestamp.from(limite));
    }
}
