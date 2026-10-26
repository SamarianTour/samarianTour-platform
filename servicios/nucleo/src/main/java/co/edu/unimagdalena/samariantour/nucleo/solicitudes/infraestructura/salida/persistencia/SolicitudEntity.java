package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Una fila de {@code solicitudes} (V3, con los campos de cada tipo de oferta de V4 y el aparte de V6). El esquema lo
 * crea Flyway (DN-01). La revisión es la versión de JPA: Hibernate actualiza con {@code WHERE revision = ?} y falla si
 * otra transacción la cambió (ADR-05).
 */
@Entity
@Table(name = "solicitudes")
public class SolicitudEntity {

    @Id
    private UUID id;

    @Column(name = "turista_id")
    private UUID turistaId;

    @Column(name = "oferta_id")
    private String ofertaId;

    @Column(name = "tipo_oferta")
    private String tipoOferta;

    @Column(name = "nombre_oferta")
    private String nombreOferta;

    @Column(name = "prestador_id")
    private UUID prestadorId;

    @Column(name = "zona_id")
    private String zonaId;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    @Column(name = "hora_reserva")
    private LocalTime horaReserva;

    @Column(name = "fecha_salida")
    private LocalDate fechaSalida;

    @Column(name = "numero_unidades")
    private Integer numeroUnidades;

    private int personas;

    @Column(name = "nota_turista")
    private String notaTurista;

    @Column(name = "operador_id")
    private UUID operadorId;

    private String estado;

    @Column(name = "precio_base")
    private Long precioBase;

    @Column(name = "politica_cancelacion")
    private String politicaCancelacion;

    @Column(name = "nota_operador")
    private String notaOperador;

    @Column(name = "aparte_hasta")
    private Instant aparteHasta;

    @Column(name = "vence_en")
    private Instant venceEn;

    @Column(name = "limite_vencimiento")
    private Instant limiteVencimiento;

    @Column(name = "intento_pago_en_curso")
    private boolean intentoPagoEnCurso;

    @Column(name = "intentos_pago_fallidos")
    private int intentosPagoFallidos;

    @Column(name = "creada_en")
    private Instant creadaEn;

    @Column(name = "actualizada_en")
    private Instant actualizadaEn;

    /** Vacía en una solicitud nueva: Spring Data la inserta en lugar de buscarla. */
    @Version
    private Long revision;

    protected SolicitudEntity() {
    }

    SolicitudEntity(UUID id, UUID turistaId, String ofertaId, String tipoOferta, String nombreOferta,
                    UUID prestadorId, String zonaId, LocalDate fechaInicio, LocalTime horaSalida,
                    LocalTime horaReserva, LocalDate fechaSalida, Integer numeroUnidades, int personas,
                    String notaTurista, UUID operadorId, Instant creadaEn) {
        this.id = id;
        this.turistaId = turistaId;
        this.ofertaId = ofertaId;
        this.tipoOferta = tipoOferta;
        this.nombreOferta = nombreOferta;
        this.prestadorId = prestadorId;
        this.zonaId = zonaId;
        this.fechaInicio = fechaInicio;
        this.horaSalida = horaSalida;
        this.horaReserva = horaReserva;
        this.fechaSalida = fechaSalida;
        this.numeroUnidades = numeroUnidades;
        this.personas = personas;
        this.notaTurista = notaTurista;
        this.operadorId = operadorId;
        this.creadaEn = creadaEn;
    }

    /** Lo que cambia a lo largo del ciclo de vida; los datos del envío no cambian nunca. */
    void cambiarEstado(String estado, Long precioBase, String politicaCancelacion, String notaOperador,
                       Instant aparteHasta, Instant venceEn, Instant limiteVencimiento, boolean intentoPagoEnCurso,
                       int intentosPagoFallidos, Instant actualizadaEn) {
        this.estado = estado;
        this.precioBase = precioBase;
        this.politicaCancelacion = politicaCancelacion;
        this.notaOperador = notaOperador;
        this.aparteHasta = aparteHasta;
        this.venceEn = venceEn;
        this.limiteVencimiento = limiteVencimiento;
        this.intentoPagoEnCurso = intentoPagoEnCurso;
        this.intentosPagoFallidos = intentosPagoFallidos;
        this.actualizadaEn = actualizadaEn;
    }

    UUID id() {
        return id;
    }

    UUID turistaId() {
        return turistaId;
    }

    String ofertaId() {
        return ofertaId;
    }

    String tipoOferta() {
        return tipoOferta;
    }

    String nombreOferta() {
        return nombreOferta;
    }

    UUID prestadorId() {
        return prestadorId;
    }

    String zonaId() {
        return zonaId;
    }

    LocalDate fechaInicio() {
        return fechaInicio;
    }

    LocalTime horaSalida() {
        return horaSalida;
    }

    LocalTime horaReserva() {
        return horaReserva;
    }

    LocalDate fechaSalida() {
        return fechaSalida;
    }

    Integer numeroUnidades() {
        return numeroUnidades;
    }

    int personas() {
        return personas;
    }

    String notaTurista() {
        return notaTurista;
    }

    UUID operadorId() {
        return operadorId;
    }

    String estado() {
        return estado;
    }

    Long precioBase() {
        return precioBase;
    }

    String politicaCancelacion() {
        return politicaCancelacion;
    }

    String notaOperador() {
        return notaOperador;
    }

    Instant aparteHasta() {
        return aparteHasta;
    }

    Instant venceEn() {
        return venceEn;
    }

    boolean intentoPagoEnCurso() {
        return intentoPagoEnCurso;
    }

    int intentosPagoFallidos() {
        return intentosPagoFallidos;
    }

    Instant creadaEn() {
        return creadaEn;
    }

    Instant actualizadaEn() {
        return actualizadaEn;
    }

    Long revision() {
        return revision;
    }
}
