package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CompromisosConTuristas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.CompromisosSolicitudesPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

/** Cuenta en la base, con un solo recorrido, los compromisos del prestador con los turistas (DN-45). */
@Component
public class CompromisosSolicitudesJdbcAdapter implements CompromisosSolicitudesPort {

    private final JdbcTemplate jdbc;

    CompromisosSolicitudesJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CompromisosConTuristas delPrestador(UUID prestadorId, LocalDate hoy) {
        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE estado = 'PAGADA' AND COALESCE(fecha_salida, fecha_inicio) >= ?),
                       count(*) FILTER (WHERE estado IN ('ATENDIDA', 'CONFIRMADA', 'PAGO_FALLIDO')
                                          AND intento_pago_en_curso)
                FROM solicitudes WHERE prestador_id = ?
                """, (fila, n) -> new CompromisosConTuristas(fila.getLong(1), fila.getLong(2)), Date.valueOf(hoy),
                prestadorId);
    }
}
