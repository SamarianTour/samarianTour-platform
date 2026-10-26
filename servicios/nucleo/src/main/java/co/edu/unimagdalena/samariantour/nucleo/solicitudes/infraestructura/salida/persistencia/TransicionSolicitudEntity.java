package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code transiciones_solicitud} (V3): el historial solo agrega filas. */
@Entity
@Table(name = "transiciones_solicitud")
public class TransicionSolicitudEntity {

    @Id
    private UUID id;

    @Column(name = "solicitud_id")
    private UUID solicitudId;

    private int orden;

    @Column(name = "estado_origen")
    private String estadoOrigen;

    @Column(name = "estado_destino")
    private String estadoDestino;

    @Column(name = "ocurrida_en")
    private Instant ocurridaEn;

    private String actor;

    @Column(name = "actor_id")
    private UUID actorId;

    private String motivo;

    protected TransicionSolicitudEntity() {
    }

    TransicionSolicitudEntity(UUID id, UUID solicitudId, int orden, String estadoOrigen, String estadoDestino,
                              Instant ocurridaEn, String actor, UUID actorId, String motivo) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.orden = orden;
        this.estadoOrigen = estadoOrigen;
        this.estadoDestino = estadoDestino;
        this.ocurridaEn = ocurridaEn;
        this.actor = actor;
        this.actorId = actorId;
        this.motivo = motivo;
    }

    UUID solicitudId() {
        return solicitudId;
    }

    int orden() {
        return orden;
    }

    String estadoOrigen() {
        return estadoOrigen;
    }

    String estadoDestino() {
        return estadoDestino;
    }

    Instant ocurridaEn() {
        return ocurridaEn;
    }

    String actor() {
        return actor;
    }

    UUID actorId() {
        return actorId;
    }

    String motivo() {
        return motivo;
    }
}
