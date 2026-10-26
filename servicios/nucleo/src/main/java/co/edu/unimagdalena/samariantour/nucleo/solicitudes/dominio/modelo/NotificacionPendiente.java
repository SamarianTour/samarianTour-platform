package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Una notificación por WhatsApp guardada y todavía sin enviar (DN-13). Si el envío falla se reprograma con una espera
 * creciente, como los avisos de la bandeja (DN-06), y a los cinco intentos queda Fallida.
 */
public final class NotificacionPendiente {

    public static final int INTENTOS_MAXIMOS = 5;
    static final Duration PRIMERA_ESPERA = Duration.ofMinutes(1);
    static final Duration ESPERA_MAXIMA = Duration.ofMinutes(30);
    public static final int LONGITUD_MAXIMA_ERROR = 500;

    private final UUID id;
    private final UUID solicitudId;
    private final Destinatario destinatario;
    private final TipoNotificacion tipo;
    private final Map<String, String> datos;
    private EstadoEnvio estado;
    private int intentos;
    private Instant proximoIntento;
    private Instant enviadaEn;
    private String ultimoError;

    public NotificacionPendiente(UUID id, UUID solicitudId, Destinatario destinatario, TipoNotificacion tipo,
                                 Map<String, String> datos, int intentos) {
        this.id = Objects.requireNonNull(id, "id");
        this.solicitudId = Objects.requireNonNull(solicitudId, "solicitudId");
        this.destinatario = Objects.requireNonNull(destinatario, "destinatario");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.datos = Map.copyOf(datos);
        this.estado = EstadoEnvio.PENDIENTE;
        this.intentos = intentos;
    }

    public void registrarEnvio(Instant ahora) {
        intentos++;
        estado = EstadoEnvio.ENVIADA;
        enviadaEn = ahora;
        ultimoError = null;
    }

    /** El envío falló: se reintenta tras 1, 2, 4... minutos, hasta 30; al quinto intento queda Fallida. */
    public void registrarFallo(String error, Instant ahora) {
        intentos++;
        ultimoError = error == null || error.length() <= LONGITUD_MAXIMA_ERROR
                ? error : error.substring(0, LONGITUD_MAXIMA_ERROR);
        if (intentos >= INTENTOS_MAXIMOS) {
            estado = EstadoEnvio.FALLIDA;
            return;
        }
        Duration espera = PRIMERA_ESPERA.multipliedBy(1L << Math.min(intentos - 1, 10));
        proximoIntento = ahora.plus(espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera);
    }

    public UUID id() {
        return id;
    }

    public UUID solicitudId() {
        return solicitudId;
    }

    public Destinatario destinatario() {
        return destinatario;
    }

    public TipoNotificacion tipo() {
        return tipo;
    }

    public Map<String, String> datos() {
        return datos;
    }

    public EstadoEnvio estado() {
        return estado;
    }

    public int intentos() {
        return intentos;
    }

    /** Vacío mientras no haya fallado. */
    public Instant proximoIntento() {
        return proximoIntento;
    }

    public Instant enviadaEn() {
        return enviadaEn;
    }

    public String ultimoError() {
        return ultimoError;
    }
}
