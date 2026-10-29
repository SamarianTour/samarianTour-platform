package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code solicitudes_supresion} (V8): a lo sumo una por cuenta. */
@Entity
@Table(name = "solicitudes_supresion")
public class SolicitudSupresionEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "fecha_solicitud")
    private Instant fechaSolicitud;

    private String estado;

    @Column(name = "fecha_ejecucion")
    private Instant fechaEjecucion;

    protected SolicitudSupresionEntity() {
    }

    SolicitudSupresionEntity(UUID id, UUID usuarioId, Instant fechaSolicitud) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.fechaSolicitud = fechaSolicitud;
    }

    void copiar(String estado, Instant fechaEjecucion) {
        this.estado = estado;
        this.fechaEjecucion = fechaEjecucion;
    }

    UUID id() {
        return id;
    }

    Instant fechaSolicitud() {
        return fechaSolicitud;
    }

    String estado() {
        return estado;
    }

    Instant fechaEjecucion() {
        return fechaEjecucion;
    }
}
