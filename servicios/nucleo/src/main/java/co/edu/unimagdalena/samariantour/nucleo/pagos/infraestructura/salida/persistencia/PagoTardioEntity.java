package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code pagos_tardios} (V7). */
@Entity
@Table(name = "pagos_tardios")
public class PagoTardioEntity {

    @Id
    private UUID id;

    @Column(name = "solicitud_id")
    private UUID solicitudId;

    @Column(name = "intento_id")
    private UUID intentoId;

    private String referencia;

    @Column(name = "transaccion_id")
    private String transaccionId;

    @Column(name = "monto_centavos")
    private long montoCentavos;

    private String motivo;

    @Column(name = "registrado_en")
    private Instant registradoEn;

    @Column(name = "aviso_enviado_en")
    private Instant avisoEnviadoEn;

    protected PagoTardioEntity() {
    }

    PagoTardioEntity(UUID id, UUID solicitudId, UUID intentoId, String referencia, String transaccionId,
                     long montoCentavos, String motivo, Instant registradoEn, Instant avisoEnviadoEn) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.intentoId = intentoId;
        this.referencia = referencia;
        this.transaccionId = transaccionId;
        this.montoCentavos = montoCentavos;
        this.motivo = motivo;
        this.registradoEn = registradoEn;
        this.avisoEnviadoEn = avisoEnviadoEn;
    }

    void marcarAvisado(Instant avisoEnviadoEn) {
        this.avisoEnviadoEn = avisoEnviadoEn;
    }

    UUID id() {
        return id;
    }

    UUID solicitudId() {
        return solicitudId;
    }

    UUID intentoId() {
        return intentoId;
    }

    String referencia() {
        return referencia;
    }

    String transaccionId() {
        return transaccionId;
    }

    long montoCentavos() {
        return montoCentavos;
    }

    String motivo() {
        return motivo;
    }

    Instant registradoEn() {
        return registradoEn;
    }

    Instant avisoEnviadoEn() {
        return avisoEnviadoEn;
    }
}
