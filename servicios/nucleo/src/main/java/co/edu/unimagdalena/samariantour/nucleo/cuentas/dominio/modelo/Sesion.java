package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Una sesión iniciada: guarda el hash del token de renovación vigente (RF-04). El token de acceso no se guarda; vence
 * solo. Cada renovación cambia el token de renovación y extiende la sesión; cerrar sesión la revoca (RF-06).
 */
public final class Sesion {

    /** Vida del token de acceso, que los servicios validan sin consultar al núcleo (DN-19). */
    public static final Duration VIGENCIA_ACCESO = Duration.ofMinutes(15);
    /** Vida del token de renovación desde la última vez que se usó (DN-19). */
    public static final Duration VIGENCIA_RENOVACION = Duration.ofDays(7);

    private final UUID id;
    private final UUID usuarioId;
    private String hashRenovacion;
    private final Instant creadaEn;
    private Instant expiraEn;
    private Instant revocadaEn;
    private final Long revision;

    public Sesion(UUID id, UUID usuarioId, String hashRenovacion, Instant creadaEn, Instant expiraEn,
                  Instant revocadaEn, Long revision) {
        this.id = Objects.requireNonNull(id, "id");
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        this.hashRenovacion = Objects.requireNonNull(hashRenovacion, "hashRenovacion");
        this.creadaEn = micros(Objects.requireNonNull(creadaEn, "creadaEn"));
        this.expiraEn = micros(Objects.requireNonNull(expiraEn, "expiraEn"));
        this.revocadaEn = micros(revocadaEn);
        this.revision = revision;
    }

    public static Sesion iniciar(UUID id, UUID usuarioId, String hashRenovacion, Instant ahora) {
        return new Sesion(id, usuarioId, hashRenovacion, ahora, ahora.plus(VIGENCIA_RENOVACION), null, null);
    }

    public boolean estaVigente(Instant ahora) {
        return revocadaEn == null && ahora.isBefore(expiraEn);
    }

    /** Cambia el token de renovación: el anterior ya no sirve. La sesión dura otros 7 días. */
    public void renovar(String nuevoHashRenovacion, Instant ahora) {
        if (!estaVigente(ahora)) {
            throw new IllegalStateException("La sesión %s ya no está vigente".formatted(id));
        }
        hashRenovacion = Objects.requireNonNull(nuevoHashRenovacion, "nuevoHashRenovacion");
        expiraEn = micros(ahora.plus(VIGENCIA_RENOVACION));
    }

    /** Cierra la sesión (RF-06). Revocar una sesión ya revocada no cambia nada. */
    public void revocar(Instant ahora) {
        if (revocadaEn == null) {
            revocadaEn = micros(ahora);
        }
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

    public String hashRenovacion() {
        return hashRenovacion;
    }

    public Instant creadaEn() {
        return creadaEn;
    }

    public Instant expiraEn() {
        return expiraEn;
    }

    public Instant revocadaEn() {
        return revocadaEn;
    }

    public Long revision() {
        return revision;
    }
}
