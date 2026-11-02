package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AlertaSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.AlertaSesgoRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Las alertas en {@code alertas_sesgo} y las revisiones en {@code revisiones_sesgo} (V15). El índice único parcial
 * deja a lo sumo una alerta abierta por prestador; la clave del día, una revisión por día aunque corran dos réplicas.
 */
@Component
public class AlertaSesgoJdbcAdapter implements AlertaSesgoRepositoryPort {

    private final JdbcTemplate jdbc;

    AlertaSesgoJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AlertaSesgo> abiertas() {
        return jdbc.query("SELECT * FROM alertas_sesgo WHERE cerrada_en IS NULL ORDER BY indice DESC, prestador_id",
                (fila, n) -> new AlertaSesgo(fila.getObject("id", UUID.class),
                        fila.getObject("prestador_id", UUID.class), fila.getBigDecimal("indice"),
                        fila.getBigDecimal("porcentaje_apariciones"), fila.getBigDecimal("porcentaje_ofertas"),
                        fila.getLong("apariciones"), fila.getLong("recomendaciones"),
                        fila.getTimestamp("abierta_en").toInstant(), fila.getTimestamp("actualizada_en").toInstant(),
                        null));
    }

    @Override
    public void guardar(AlertaSesgo alerta) {
        jdbc.update("""
                        INSERT INTO alertas_sesgo (id, prestador_id, indice, porcentaje_apariciones, porcentaje_ofertas,
                                                   apariciones, recomendaciones, abierta_en, actualizada_en, cerrada_en)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET indice = EXCLUDED.indice,
                            porcentaje_apariciones = EXCLUDED.porcentaje_apariciones,
                            porcentaje_ofertas = EXCLUDED.porcentaje_ofertas, apariciones = EXCLUDED.apariciones,
                            recomendaciones = EXCLUDED.recomendaciones, actualizada_en = EXCLUDED.actualizada_en,
                            cerrada_en = EXCLUDED.cerrada_en""",
                alerta.id(), alerta.prestadorId(), alerta.indice(), alerta.porcentajeApariciones(),
                alerta.porcentajeOfertas(), alerta.apariciones(), alerta.recomendaciones(),
                Timestamp.from(alerta.abiertaEn()), Timestamp.from(alerta.actualizadaEn()),
                alerta.cerradaEn() == null ? null : Timestamp.from(alerta.cerradaEn()));
    }

    @Override
    public boolean revisado(LocalDate dia) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM revisiones_sesgo WHERE dia = ?)",
                Boolean.class, Date.valueOf(dia)));
    }

    @Override
    public boolean reservarRevision(LocalDate dia, Instant ahora) {
        return jdbc.update("INSERT INTO revisiones_sesgo (dia, revisada_en) VALUES (?, ?) ON CONFLICT DO NOTHING",
                Date.valueOf(dia), Timestamp.from(ahora)) == 1;
    }
}
