package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.PoliticaReintentos;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Un intento de pagar una solicitud en la pasarela (RF-46 a RF-52, RF-58, DN-20). El monto es el precio base exacto,
 * en centavos de peso, sin cargos adicionales (RF-49). La referencia es la que la pasarela devuelve en sus eventos.
 */
public final class IntentoPago {

    public static final String MONEDA = "COP";

    private final UUID id;
    private final UUID solicitudId;
    private final UUID turistaId;
    private final int numero;
    private final String referencia;
    private final long montoCentavos;
    private final Instant creadoEn;
    private Instant abandonarEn;
    private EstadoIntento estado;
    private String transaccionId;
    private Instant resueltoEn;
    private final Long revision;
    private boolean cambio;

    private IntentoPago(UUID id, UUID solicitudId, UUID turistaId, int numero, String referencia, long montoCentavos,
                        Instant creadoEn, Instant abandonarEn, EstadoIntento estado, String transaccionId,
                        Instant resueltoEn, Long revision) {
        this.id = Objects.requireNonNull(id, "id");
        this.solicitudId = Objects.requireNonNull(solicitudId, "solicitudId");
        this.turistaId = Objects.requireNonNull(turistaId, "turistaId");
        this.numero = numero;
        this.referencia = Objects.requireNonNull(referencia, "referencia");
        this.montoCentavos = montoCentavos;
        this.creadoEn = micros(Objects.requireNonNull(creadoEn, "creadoEn"));
        this.abandonarEn = micros(Objects.requireNonNull(abandonarEn, "abandonarEn"));
        this.estado = Objects.requireNonNull(estado, "estado");
        this.transaccionId = transaccionId;
        this.resueltoEn = micros(resueltoEn);
        this.revision = revision;
    }

    /**
     * Abre un intento por el precio base de la solicitud (RF-46, RF-49). Desde ya queda cuándo se abandona si no tiene
     * resultado, con la gracia de la ventana (DN-20).
     *
     * @param numero  el orden del intento en la solicitud, desde 1
     * @param venceEn el fin de la ventana de la solicitud; vacío si no se conoce
     */
    public static IntentoPago nuevo(UUID id, UUID solicitudId, UUID turistaId, int numero, long precioBase,
                                    Instant venceEn, Instant ahora) {
        if (precioBase <= 0) {
            throw new IllegalArgumentException("El precio base debe ser positivo");
        }
        return new IntentoPago(id, solicitudId, turistaId, numero, referenciaDe(id),
                Math.multiplyExact(precioBase, 100L), ahora, PoliticaReintentos.abandonoDe(ahora, venceEn),
                EstadoIntento.EN_CURSO, null, null, null);
    }

    public static IntentoPago reconstituir(UUID id, UUID solicitudId, UUID turistaId, int numero, String referencia,
                                           long montoCentavos, Instant creadoEn, Instant abandonarEn,
                                           EstadoIntento estado, String transaccionId, Instant resueltoEn,
                                           long revision) {
        return new IntentoPago(id, solicitudId, turistaId, numero, referencia, montoCentavos, creadoEn, abandonarEn,
                estado, transaccionId, resueltoEn, revision);
    }

    /** Única por intento [SUPUESTO]: la pasarela la exige única por transacción (hasta 255 caracteres). */
    static String referenciaDe(UUID id) {
        return "ST-" + id;
    }

    /**
     * Aplica lo que informó la pasarela. Guarda el id de la transacción (RF-52): el de la que lo resuelve, o el de la
     * primera que informa algo. Un estado final lo resuelve; uno ya resuelto no cambia, salvo un abandonado, porque su
     * resultado llegó tarde pero es el real. Una aprobación de otra transacción sobre un intento con resultado no lo
     * cambia, pero se informa, porque ese dinero no paga nada.
     *
     * @return el resultado que hay que llevar a la solicitud
     */
    public ResultadoIntento aplicar(EstadoTransaccion transaccion, String idTransaccion, Instant ahora) {
        if (transaccion == EstadoTransaccion.APROBADA && estado.tieneResultado()
                && !Objects.equals(idTransaccion, transaccionId)) {
            return ResultadoIntento.APROBADO_SIN_EFECTO;
        }
        if (idTransaccion != null && transaccionId == null) {
            transaccionId = idTransaccion;
            cambio = true;
        }
        if (!transaccion.esFinal() || (estado != EstadoIntento.EN_CURSO && estado != EstadoIntento.ABANDONADO)) {
            return ResultadoIntento.SIN_CAMBIOS;
        }
        estado = switch (transaccion) {
            case APROBADA -> EstadoIntento.APROBADO;
            case ERROR -> EstadoIntento.ERROR;
            default -> EstadoIntento.RECHAZADO;
        };
        if (idTransaccion != null) {
            transaccionId = idTransaccion;
        }
        resueltoEn = micros(ahora);
        cambio = true;
        return estado == EstadoIntento.APROBADO ? ResultadoIntento.APROBADO : ResultadoIntento.RECHAZADO;
    }

    /**
     * Un intento sin resultado a su hora de abandono queda abandonado (DN-20): el turista cerró el pago o no lo
     * terminó. Deja de aplazar el vencimiento de la solicitud.
     *
     * @return true si lo abandonó ahora
     */
    public boolean abandonarSiCorresponde(Instant ahora) {
        if (estado != EstadoIntento.EN_CURSO || ahora.isBefore(abandonarEn)) {
            return false;
        }
        estado = EstadoIntento.ABANDONADO;
        resueltoEn = micros(ahora);
        cambio = true;
        return true;
    }

    /**
     * Aplaza el abandono de un intento que no se pudo abandonar por un error inesperado (DN-20), para que la tarea siga
     * con los demás y lo intente otra vez después.
     */
    public void aplazarAbandono(Instant ahora) {
        abandonarEn = micros(ahora.plus(PoliticaReintentos.ESPERA_TRAS_ERROR));
        cambio = true;
    }

    private static Instant micros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    public boolean tieneCambios() {
        return cambio;
    }

    public UUID id() {
        return id;
    }

    public UUID solicitudId() {
        return solicitudId;
    }

    public UUID turistaId() {
        return turistaId;
    }

    public int numero() {
        return numero;
    }

    public String referencia() {
        return referencia;
    }

    public long montoCentavos() {
        return montoCentavos;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    /** Cuándo se abandona si sigue sin resultado (DN-20). */
    public Instant abandonarEn() {
        return abandonarEn;
    }

    public EstadoIntento estado() {
        return estado;
    }

    /** El identificador de la transacción en la pasarela, desde su primer evento (RF-52). */
    public String transaccionId() {
        return transaccionId;
    }

    public Instant resueltoEn() {
        return resueltoEn;
    }

    /** La revisión leída de la base para el bloqueo optimista (ADR-05); vacía si todavía no se guardó. */
    public Long revision() {
        return revision;
    }
}
