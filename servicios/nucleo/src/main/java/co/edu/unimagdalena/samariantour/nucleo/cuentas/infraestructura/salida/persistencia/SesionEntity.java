package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code sesiones} (V8), con bloqueo optimista: un token de renovación no se usa dos veces a la vez. */
@Entity
@Table(name = "sesiones")
public class SesionEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "hash_renovacion")
    private String hashRenovacion;

    @Column(name = "creada_en")
    private Instant creadaEn;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @Column(name = "revocada_en")
    private Instant revocadaEn;

    @Version
    private Long revision;

    protected SesionEntity() {
    }

    SesionEntity(UUID id, UUID usuarioId, Instant creadaEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.creadaEn = creadaEn;
    }

    void copiar(String hashRenovacion, Instant expiraEn, Instant revocadaEn) {
        this.hashRenovacion = hashRenovacion;
        this.expiraEn = expiraEn;
        this.revocadaEn = revocadaEn;
    }

    UUID id() {
        return id;
    }

    UUID usuarioId() {
        return usuarioId;
    }

    String hashRenovacion() {
        return hashRenovacion;
    }

    Instant creadaEn() {
        return creadaEn;
    }

    Instant expiraEn() {
        return expiraEn;
    }

    Instant revocadaEn() {
        return revocadaEn;
    }

    Long revision() {
        return revision;
    }
}
