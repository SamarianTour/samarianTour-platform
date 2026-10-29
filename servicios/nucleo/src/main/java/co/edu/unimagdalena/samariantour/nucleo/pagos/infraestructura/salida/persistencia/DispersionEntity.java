package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una fila de {@code dispersiones} (V7). Sin revisión: solo la cambia la tarea de dispersiones, que la toma bloqueada
 * con SKIP LOCKED.
 */
@Entity
@Table(name = "dispersiones")
public class DispersionEntity {

    @Id
    private UUID id;

    @Column(name = "solicitud_id")
    private UUID solicitudId;

    @Column(name = "prestador_id")
    private UUID prestadorId;

    @Column(name = "monto_bruto")
    private long montoBruto;

    @Column(name = "porcentaje_comision")
    private BigDecimal porcentajeComision;

    private long comision;

    @Column(name = "costo_transaccional")
    private long costoTransaccional;

    @Column(name = "monto_neto")
    private long montoNeto;

    private String estado;

    private int intentos;

    @Column(name = "ultima_causa")
    private String ultimaCausa;

    @Column(name = "cuenta_destino")
    private String cuentaDestino;

    @Column(name = "id_pasarela")
    private String idPasarela;

    @Column(name = "creada_en")
    private Instant creadaEn;

    @Column(name = "proximo_intento")
    private Instant proximoIntento;

    @Column(name = "ejecutada_en")
    private Instant ejecutadaEn;

    @Column(name = "aviso_enviado_en")
    private Instant avisoEnviadoEn;

    protected DispersionEntity() {
    }

    DispersionEntity(UUID id, UUID solicitudId, UUID prestadorId, long montoBruto, BigDecimal porcentajeComision,
                     long comision, long costoTransaccional, long montoNeto, Instant creadaEn) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.prestadorId = prestadorId;
        this.montoBruto = montoBruto;
        this.porcentajeComision = porcentajeComision;
        this.comision = comision;
        this.costoTransaccional = costoTransaccional;
        this.montoNeto = montoNeto;
        this.creadaEn = creadaEn;
    }

    /** Lo que cambia al ejecutarla; la liquidación no cambia nunca (RF-83). */
    void cambiarEstado(String estado, int intentos, String ultimaCausa, String cuentaDestino, String idPasarela,
                       Instant proximoIntento, Instant ejecutadaEn, Instant avisoEnviadoEn) {
        this.estado = estado;
        this.intentos = intentos;
        this.ultimaCausa = ultimaCausa;
        this.cuentaDestino = cuentaDestino;
        this.idPasarela = idPasarela;
        this.proximoIntento = proximoIntento;
        this.ejecutadaEn = ejecutadaEn;
        this.avisoEnviadoEn = avisoEnviadoEn;
    }

    UUID id() {
        return id;
    }

    UUID solicitudId() {
        return solicitudId;
    }

    UUID prestadorId() {
        return prestadorId;
    }

    long montoBruto() {
        return montoBruto;
    }

    BigDecimal porcentajeComision() {
        return porcentajeComision;
    }

    long comision() {
        return comision;
    }

    long costoTransaccional() {
        return costoTransaccional;
    }

    long montoNeto() {
        return montoNeto;
    }

    String estado() {
        return estado;
    }

    int intentos() {
        return intentos;
    }

    String ultimaCausa() {
        return ultimaCausa;
    }

    String cuentaDestino() {
        return cuentaDestino;
    }

    String idPasarela() {
        return idPasarela;
    }

    Instant creadaEn() {
        return creadaEn;
    }

    Instant proximoIntento() {
        return proximoIntento;
    }

    Instant ejecutadaEn() {
        return ejecutadaEn;
    }

    Instant avisoEnviadoEn() {
        return avisoEnviadoEn;
    }
}
