package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Una fila de {@code aceptaciones_politica} (V8). Solo se insertan. */
@Entity
@Table(name = "aceptaciones_politica")
@IdClass(AceptacionPoliticaEntity.Clave.class)
public class AceptacionPoliticaEntity {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Id
    private String version;

    @Column(name = "fecha_aceptacion")
    private Instant fechaAceptacion;

    protected AceptacionPoliticaEntity() {
    }

    AceptacionPoliticaEntity(UUID usuarioId, String version, Instant fechaAceptacion) {
        this.usuarioId = usuarioId;
        this.version = version;
        this.fechaAceptacion = fechaAceptacion;
    }

    String version() {
        return version;
    }

    Instant fechaAceptacion() {
        return fechaAceptacion;
    }

    /** La clave compuesta: una aceptación por usuario y versión. */
    public static class Clave implements Serializable {

        private UUID usuarioId;
        private String version;

        protected Clave() {
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave clave && Objects.equals(usuarioId, clave.usuarioId)
                    && Objects.equals(version, clave.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioId, version);
        }
    }
}
