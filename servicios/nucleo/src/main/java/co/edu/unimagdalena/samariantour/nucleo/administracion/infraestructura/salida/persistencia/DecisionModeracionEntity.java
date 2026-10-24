package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code decisiones_moderacion} (V2). El esquema lo crea Flyway (DN-01). */
@Entity
@Table(name = "decisiones_moderacion")
public class DecisionModeracionEntity {

    @Id
    private UUID id;

    private String tipo;

    @Column(name = "moderado_id")
    private String moderadoId;

    @Column(name = "numero_version")
    private Integer numeroVersion;

    private String decision;

    private String motivo;

    @Column(name = "administrador_id")
    private UUID administradorId;

    @Column(name = "decidida_en")
    private Instant decididaEn;

    @Column(name = "id_aviso")
    private String idAviso;

    protected DecisionModeracionEntity() {
    }

    String tipo() {
        return tipo;
    }

    String moderadoId() {
        return moderadoId;
    }

    Integer numeroVersion() {
        return numeroVersion;
    }

    String decision() {
        return decision;
    }

    String motivo() {
        return motivo;
    }

    UUID administradorId() {
        return administradorId;
    }

    Instant decididaEn() {
        return decididaEn;
    }

    String idAviso() {
        return idAviso;
    }
}
