package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.AuditoriaPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/** La auditoría en {@code auditoria_prestadores} (V10). Solo se insertan filas, así que basta con JDBC. */
@Component
public class AuditoriaPrestadoresAdapter implements AuditoriaPort {

    private final JdbcTemplate jdbc;

    AuditoriaPrestadoresAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(UUID prestadorId, String accion, String detalle, Instant ocurridoEn) {
        jdbc.update("INSERT INTO auditoria_prestadores (id, prestador_id, accion, detalle, ocurrido_en) "
                + "VALUES (?, ?, ?, ?, ?)", UUID.randomUUID(), prestadorId, accion, detalle,
                Timestamp.from(ocurridoEn));
    }
}
