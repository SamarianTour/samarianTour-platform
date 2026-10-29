package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.PoliticaReintentos;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * La transferencia al prestador de lo que pagó el turista, descontadas la comisión y el costo transaccional (RF-53,
 * RF-54, DN-24). Nace pendiente al confirmarse el pago, con la liquidación de ese momento, que ya no cambia (RF-83).
 * La cuenta destino se toma al ejecutarla, la vigente en ese momento (RF-74), y solo se guarda enmascarada (RF-75).
 */
public final class Dispersion {

    private final UUID id;
    private final UUID solicitudId;
    private final UUID prestadorId;
    private final Liquidacion liquidacion;
    private final Instant creadaEn;
    private EstadoDispersion estado;
    private int intentos;
    private String ultimaCausa;
    private String cuentaDestino;
    private String idPasarela;
    private Instant proximoIntento;
    private Instant ejecutadaEn;
    private Instant avisoEnviadoEn;

    private Dispersion(UUID id, UUID solicitudId, UUID prestadorId, Liquidacion liquidacion, Instant creadaEn,
                       EstadoDispersion estado, int intentos, String ultimaCausa, String cuentaDestino,
                       String idPasarela, Instant proximoIntento, Instant ejecutadaEn, Instant avisoEnviadoEn) {
        this.id = Objects.requireNonNull(id, "id");
        this.solicitudId = Objects.requireNonNull(solicitudId, "solicitudId");
        this.prestadorId = Objects.requireNonNull(prestadorId, "prestadorId");
        this.liquidacion = Objects.requireNonNull(liquidacion, "liquidacion");
        this.creadaEn = micros(Objects.requireNonNull(creadaEn, "creadaEn"));
        this.estado = Objects.requireNonNull(estado, "estado");
        this.intentos = intentos;
        this.ultimaCausa = ultimaCausa;
        this.cuentaDestino = cuentaDestino;
        this.idPasarela = idPasarela;
        this.proximoIntento = micros(proximoIntento);
        this.ejecutadaEn = micros(ejecutadaEn);
        this.avisoEnviadoEn = micros(avisoEnviadoEn);
    }

    /** Pendiente para ejecutarse de inmediato, con la liquidación del pago confirmado (RF-53). */
    public static Dispersion pendiente(UUID id, UUID solicitudId, UUID prestadorId, Liquidacion liquidacion,
                                       Instant ahora) {
        return new Dispersion(id, solicitudId, prestadorId, liquidacion, ahora, EstadoDispersion.PENDIENTE, 0, null,
                null, null, ahora, null, null);
    }

    public static Dispersion reconstituir(UUID id, UUID solicitudId, UUID prestadorId, Liquidacion liquidacion,
                                          Instant creadaEn, EstadoDispersion estado, int intentos, String ultimaCausa,
                                          String cuentaDestino, String idPasarela, Instant proximoIntento,
                                          Instant ejecutadaEn, Instant avisoEnviadoEn) {
        return new Dispersion(id, solicitudId, prestadorId, liquidacion, creadaEn, estado, intentos, ultimaCausa,
                cuentaDestino, idPasarela, proximoIntento, ejecutadaEn, avisoEnviadoEn);
    }

    /** La pasarela confirmó la transferencia a la cuenta (ya enmascarada). */
    public void registrarExito(String cuentaEnmascarada, String idPasarela, Instant ahora) {
        exigirPendiente();
        intentos++;
        this.cuentaDestino = cuentaEnmascarada;
        this.idPasarela = idPasarela;
        this.ultimaCausa = null;
        this.ejecutadaEn = micros(ahora);
        this.proximoIntento = null;
        this.estado = EstadoDispersion.EXITOSA;
    }

    /**
     * La transferencia falló: se reintenta hasta tres veces con espera creciente; al agotarlas queda fallida y hay que
     * avisar al prestador (RF-54).
     *
     * @param cuentaEnmascarada la cuenta que se intentó, o vacía si el prestador no tiene una
     * @param esperaBase        la espera antes del primer reintento; se duplica en cada uno
     */
    public void registrarFallo(String causa, String cuentaEnmascarada, Instant ahora, Duration esperaBase) {
        exigirPendiente();
        intentos++;
        this.ultimaCausa = causa;
        this.cuentaDestino = cuentaEnmascarada;
        this.ejecutadaEn = micros(ahora);
        if (intentos > PoliticaReintentos.REINTENTOS_DISPERSION) {
            estado = EstadoDispersion.FALLIDA;
            proximoIntento = null;
        } else {
            proximoIntento = micros(ahora.plus(PoliticaReintentos.esperaDispersion(intentos, esperaBase)));
        }
    }

    /** Ya se avisó al prestador que la dispersión falló (RF-54). */
    public void registrarAviso(Instant ahora) {
        this.avisoEnviadoEn = micros(ahora);
    }

    private void exigirPendiente() {
        if (estado != EstadoDispersion.PENDIENTE) {
            throw new IllegalStateException("La dispersión " + id + " ya está " + estado);
        }
    }

    private static Instant micros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    public UUID id() {
        return id;
    }

    public UUID solicitudId() {
        return solicitudId;
    }

    public UUID prestadorId() {
        return prestadorId;
    }

    public Liquidacion liquidacion() {
        return liquidacion;
    }

    public long montoBruto() {
        return liquidacion.montoBruto();
    }

    public BigDecimal porcentajeComision() {
        return liquidacion.porcentajeComision();
    }

    public long montoNeto() {
        return liquidacion.montoNeto();
    }

    public Instant creadaEn() {
        return creadaEn;
    }

    public EstadoDispersion estado() {
        return estado;
    }

    /** Cuántas veces se intentó, contando el primer envío. */
    public int intentos() {
        return intentos;
    }

    public String ultimaCausa() {
        return ultimaCausa;
    }

    /** Solo los últimos 4 dígitos de la cuenta a la que se envió (RF-75); vacía antes del primer intento. */
    public String cuentaDestino() {
        return cuentaDestino;
    }

    public String idPasarela() {
        return idPasarela;
    }

    /** Cuándo toca el próximo intento; vacío si ya terminó. */
    public Instant proximoIntento() {
        return proximoIntento;
    }

    public Instant ejecutadaEn() {
        return ejecutadaEn;
    }

    public Instant avisoEnviadoEn() {
        return avisoEnviadoEn;
    }
}
