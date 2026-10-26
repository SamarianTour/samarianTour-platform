package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Una fila de {@code claves_idempotencia} (V3). Se escribe con consultas nativas; la entidad sirve para leer. */
@Entity
@Table(name = "claves_idempotencia")
@IdClass(ClaveIdempotenciaEntity.Llave.class)
public class ClaveIdempotenciaEntity {

    /** La llave primaria: una clave por usuario. */
    public static class Llave implements Serializable {

        private UUID usuarioId;
        private UUID clave;

        public Llave() {
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Llave llave && Objects.equals(usuarioId, llave.usuarioId)
                    && Objects.equals(clave, llave.clave);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioId, clave);
        }
    }

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Id
    private UUID clave;

    private String operacion;

    private String huella;

    @Column(name = "codigo_respuesta")
    private Integer codigoRespuesta;

    @Column(name = "cuerpo_respuesta")
    private String cuerpoRespuesta;

    @Column(name = "creada_en")
    private Instant creadaEn;

    @Column(name = "expira_en")
    private Instant expiraEn;

    protected ClaveIdempotenciaEntity() {
    }

    String operacion() {
        return operacion;
    }

    String huella() {
        return huella;
    }

    Integer codigoRespuesta() {
        return codigoRespuesta;
    }

    String cuerpoRespuesta() {
        return cuerpoRespuesta;
    }

    Instant expiraEn() {
        return expiraEn;
    }
}
