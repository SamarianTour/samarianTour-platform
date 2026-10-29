package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code configuracion_comision} (V7): solo se agregan. */
@Entity
@Table(name = "configuracion_comision")
public class ConfiguracionComisionEntity {

    @Id
    private UUID id;

    private BigDecimal porcentaje;

    @Column(name = "vigente_desde")
    private Instant vigenteDesde;

    @Column(name = "administrador_id")
    private UUID administradorId;

    protected ConfiguracionComisionEntity() {
    }

    ConfiguracionComisionEntity(UUID id, BigDecimal porcentaje, Instant vigenteDesde, UUID administradorId) {
        this.id = id;
        this.porcentaje = porcentaje;
        this.vigenteDesde = vigenteDesde;
        this.administradorId = administradorId;
    }

    BigDecimal porcentaje() {
        return porcentaje;
    }

    Instant vigenteDesde() {
        return vigenteDesde;
    }

    UUID administradorId() {
        return administradorId;
    }
}
