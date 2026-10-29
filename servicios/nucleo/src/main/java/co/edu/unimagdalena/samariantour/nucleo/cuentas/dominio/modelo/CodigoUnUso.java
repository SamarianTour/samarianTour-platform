package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.politica.PoliticaCodigoUnUso;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Un código temporal de un solo uso: para verificar el WhatsApp o para restablecer o establecer la contraseña (RF-03,
 * RF-07, RF-08, RF-81). Solo se guarda su hash, nunca el código.
 */
public final class CodigoUnUso {

    private final UUID id;
    private final UUID usuarioId;
    private final PropositoCodigo proposito;
    private final String hashCodigo;
    private final Instant emitidoEn;
    private final Instant expiraEn;
    private Instant usadoEn;
    private int intentosFallidos;
    private final Long revision;

    public CodigoUnUso(UUID id, UUID usuarioId, PropositoCodigo proposito, String hashCodigo, Instant emitidoEn,
                       Instant expiraEn, Instant usadoEn, int intentosFallidos, Long revision) {
        this.id = Objects.requireNonNull(id, "id");
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        this.proposito = Objects.requireNonNull(proposito, "proposito");
        this.hashCodigo = Objects.requireNonNull(hashCodigo, "hashCodigo");
        this.emitidoEn = micros(Objects.requireNonNull(emitidoEn, "emitidoEn"));
        this.expiraEn = micros(Objects.requireNonNull(expiraEn, "expiraEn"));
        this.usadoEn = micros(usadoEn);
        this.intentosFallidos = intentosFallidos;
        this.revision = revision;
    }

    /** Un código nuevo, que vence a los 30 minutos (RF-08). */
    public static CodigoUnUso emitir(UUID id, UUID usuarioId, PropositoCodigo proposito, String hashCodigo,
                                     Instant ahora) {
        return new CodigoUnUso(id, usuarioId, proposito, hashCodigo, ahora, PoliticaCodigoUnUso.expiraEn(ahora), null,
                0, null);
    }

    /** Válido si no venció, no se usó y no agotó sus intentos (RF-08). */
    public boolean esValido(Instant ahora) {
        return usadoEn == null && ahora.isBefore(expiraEn)
                && intentosFallidos < PoliticaCodigoUnUso.INTENTOS_MAXIMOS;
    }

    /** Lo gasta: no sirve otra vez (RF-08). */
    public void usar(Instant ahora) {
        if (!esValido(ahora)) {
            throw new IllegalStateException("El código %s ya no es válido".formatted(id));
        }
        usadoEn = micros(ahora);
    }

    /** Alguien escribió otro código; al tercero queda invalidado. */
    public void registrarIntentoFallido() {
        intentosFallidos++;
    }

    /** Cuántos códigos equivocados más admite antes de quedar invalidado. */
    public int intentosRestantes() {
        return Math.max(0, PoliticaCodigoUnUso.INTENTOS_MAXIMOS - intentosFallidos);
    }

    private static Instant micros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    public UUID id() {
        return id;
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public PropositoCodigo proposito() {
        return proposito;
    }

    public String hashCodigo() {
        return hashCodigo;
    }

    public Instant emitidoEn() {
        return emitidoEn;
    }

    public Instant expiraEn() {
        return expiraEn;
    }

    public Instant usadoEn() {
        return usadoEn;
    }

    public int intentosFallidos() {
        return intentosFallidos;
    }

    public Long revision() {
        return revision;
    }
}
