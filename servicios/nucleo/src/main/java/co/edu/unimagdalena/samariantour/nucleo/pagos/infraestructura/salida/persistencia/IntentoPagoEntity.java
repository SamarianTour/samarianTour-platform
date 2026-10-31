package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code intentos_pago} (V7). La revisión es la versión de JPA (ADR-05). */
@Entity
@Table(name = "intentos_pago")
public class IntentoPagoEntity {

    @Id
    private UUID id;

    @Column(name = "solicitud_id")
    private UUID solicitudId;

    @Column(name = "turista_id")
    private UUID turistaId;

    private int numero;

    private String referencia;

    @Column(name = "monto_centavos")
    private long montoCentavos;

    private String estado;

    @Column(name = "transaccion_id")
    private String transaccionId;

    @Column(name = "creado_en")
    private Instant creadoEn;

    @Column(name = "abandonar_en")
    private Instant abandonarEn;

    @Column(name = "resuelto_en")
    private Instant resueltoEn;

    /** Vacía en un intento nuevo: Spring Data lo inserta en lugar de buscarlo. */
    @Version
    private Long revision;

    protected IntentoPagoEntity() {
    }

    IntentoPagoEntity(UUID id, UUID solicitudId, UUID turistaId, int numero, String referencia, long montoCentavos,
                      Instant creadoEn) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.turistaId = turistaId;
        this.numero = numero;
        this.referencia = referencia;
        this.montoCentavos = montoCentavos;
        this.creadoEn = creadoEn;
    }

    void cambiarEstado(String estado, String transaccionId, Instant resueltoEn, Instant abandonarEn) {
        this.abandonarEn = abandonarEn;
        this.estado = estado;
        this.transaccionId = transaccionId;
        this.resueltoEn = resueltoEn;
    }

    UUID id() {
        return id;
    }

    UUID solicitudId() {
        return solicitudId;
    }

    UUID turistaId() {
        return turistaId;
    }

    int numero() {
        return numero;
    }

    String referencia() {
        return referencia;
    }

    long montoCentavos() {
        return montoCentavos;
    }

    String estado() {
        return estado;
    }

    String transaccionId() {
        return transaccionId;
    }

    Instant creadoEn() {
        return creadoEn;
    }

    Instant abandonarEn() {
        return abandonarEn;
    }

    Instant resueltoEn() {
        return resueltoEn;
    }

    Long revision() {
        return revision;
    }
}
