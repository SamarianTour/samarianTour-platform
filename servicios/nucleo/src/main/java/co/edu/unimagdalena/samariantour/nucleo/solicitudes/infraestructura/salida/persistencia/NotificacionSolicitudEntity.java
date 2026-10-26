package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code notificaciones_solicitud} (V3). El esquema lo crea Flyway (DN-01). */
@Entity
@Table(name = "notificaciones_solicitud")
public class NotificacionSolicitudEntity {

    @Id
    private UUID id;

    @Column(name = "solicitud_id")
    private UUID solicitudId;

    @Column(name = "destinatario_tipo")
    private String destinatarioTipo;

    @Column(name = "destinatario_id")
    private UUID destinatarioId;

    private String canal;

    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    private String datos;

    private String estado;

    private int intentos;

    @Column(name = "proximo_intento")
    private Instant proximoIntento;

    @Column(name = "creada_en")
    private Instant creadaEn;

    @Column(name = "enviada_en")
    private Instant enviadaEn;

    @Column(name = "ultimo_error")
    private String ultimoError;

    protected NotificacionSolicitudEntity() {
    }

    NotificacionSolicitudEntity(UUID id, UUID solicitudId, String destinatarioTipo, UUID destinatarioId, String canal,
                                String tipo, String datos, String estado, Instant proximoIntento, Instant creadaEn,
                                Instant enviadaEn) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.destinatarioTipo = destinatarioTipo;
        this.destinatarioId = destinatarioId;
        this.canal = canal;
        this.tipo = tipo;
        this.datos = datos;
        this.estado = estado;
        this.proximoIntento = proximoIntento;
        this.creadaEn = creadaEn;
        this.enviadaEn = enviadaEn;
    }

    void registrarIntento(String estado, int intentos, Instant proximoIntento, Instant enviadaEn, String ultimoError) {
        this.estado = estado;
        this.intentos = intentos;
        if (proximoIntento != null) {
            this.proximoIntento = proximoIntento;
        }
        this.enviadaEn = enviadaEn;
        this.ultimoError = ultimoError;
    }

    UUID id() {
        return id;
    }

    UUID solicitudId() {
        return solicitudId;
    }

    String destinatarioTipo() {
        return destinatarioTipo;
    }

    UUID destinatarioId() {
        return destinatarioId;
    }

    String tipo() {
        return tipo;
    }

    String datos() {
        return datos;
    }

    int intentos() {
        return intentos;
    }

    Instant creadaEn() {
        return creadaEn;
    }
}
