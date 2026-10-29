package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code cuentas_cobro} (V10). El número y el titular llegan aquí ya cifrados (RNF-08). */
@Entity
@Table(name = "cuentas_cobro")
public class CuentaCobroEntity {

    @Id
    private UUID id;

    @Column(name = "prestador_id")
    private UUID prestadorId;

    private String tipo;

    private String entidad;

    @Column(name = "numero_cifrado")
    private String numeroCifrado;

    @Column(name = "titular_cifrado")
    private String titularCifrado;

    @Column(name = "vigente_desde")
    private Instant vigenteDesde;

    @Column(name = "vigente_hasta")
    private Instant vigenteHasta;

    protected CuentaCobroEntity() {
    }

    CuentaCobroEntity(UUID id, UUID prestadorId, String tipo, String entidad, String numeroCifrado,
                      String titularCifrado, Instant vigenteDesde, Instant vigenteHasta) {
        this.id = id;
        this.prestadorId = prestadorId;
        this.tipo = tipo;
        this.entidad = entidad;
        this.numeroCifrado = numeroCifrado;
        this.titularCifrado = titularCifrado;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
    }

    void cerrar(Instant hasta) {
        this.vigenteHasta = hasta;
    }

    UUID id() {
        return id;
    }

    UUID prestadorId() {
        return prestadorId;
    }

    String tipo() {
        return tipo;
    }

    String entidad() {
        return entidad;
    }

    String numeroCifrado() {
        return numeroCifrado;
    }

    String titularCifrado() {
        return titularCifrado;
    }

    Instant vigenteDesde() {
        return vigenteDesde;
    }

    Instant vigenteHasta() {
        return vigenteHasta;
    }
}
