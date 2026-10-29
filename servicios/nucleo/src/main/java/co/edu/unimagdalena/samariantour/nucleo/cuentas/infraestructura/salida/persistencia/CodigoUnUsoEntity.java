package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code codigos_un_uso} (V8), con bloqueo optimista: un código no se gasta dos veces a la vez. */
@Entity
@Table(name = "codigos_un_uso")
public class CodigoUnUsoEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    private String proposito;

    @Column(name = "hash_codigo")
    private String hashCodigo;

    @Column(name = "emitido_en")
    private Instant emitidoEn;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "intentos_fallidos")
    private int intentosFallidos;

    @Version
    private Long revision;

    protected CodigoUnUsoEntity() {
    }

    CodigoUnUsoEntity(UUID id, UUID usuarioId, String proposito, String hashCodigo, Instant emitidoEn,
                      Instant expiraEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.proposito = proposito;
        this.hashCodigo = hashCodigo;
        this.emitidoEn = emitidoEn;
        this.expiraEn = expiraEn;
    }

    void copiar(Instant usadoEn, int intentosFallidos) {
        this.usadoEn = usadoEn;
        this.intentosFallidos = intentosFallidos;
    }

    UUID id() {
        return id;
    }

    UUID usuarioId() {
        return usuarioId;
    }

    String proposito() {
        return proposito;
    }

    String hashCodigo() {
        return hashCodigo;
    }

    Instant emitidoEn() {
        return emitidoEn;
    }

    Instant expiraEn() {
        return expiraEn;
    }

    Instant usadoEn() {
        return usadoEn;
    }

    int intentosFallidos() {
        return intentosFallidos;
    }

    Long revision() {
        return revision;
    }
}
