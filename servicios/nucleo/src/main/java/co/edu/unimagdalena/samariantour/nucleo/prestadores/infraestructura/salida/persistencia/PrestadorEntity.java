package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code prestadores} (V10). El WhatsApp llega aquí ya cifrado (RNF-08). */
@Entity
@Table(name = "prestadores")
public class PrestadorEntity {

    @Id
    private UUID id;

    @Column(name = "numero_rnt")
    private String numeroRnt;

    private String tipo;

    @Column(name = "numero_empleados")
    private Integer numeroEmpleados;

    private String modalidades;

    private String idiomas;

    @Column(name = "razon_social")
    private String razonSocial;

    private String municipio;

    @Column(name = "categoria_rnt")
    private String categoriaRnt;

    @Column(name = "persona_natural_rnt")
    private boolean personaNaturalRnt;

    @Column(name = "estado_rnt")
    private String estadoRnt;

    @Column(name = "visto_en_rnt")
    private boolean vistoEnRnt;

    @Column(name = "ultima_sincronizacion")
    private Instant ultimaSincronizacion;

    @Column(name = "whatsapp_cifrado")
    private String whatsappCifrado;

    @Column(name = "whatsapp_verificado")
    private boolean whatsappVerificado;

    @Column(name = "whatsapp_codigo_hash")
    private String codigoHash;

    @Column(name = "whatsapp_codigo_emitido")
    private Instant codigoEmitido;

    @Column(name = "whatsapp_codigo_intentos")
    private int codigoIntentos;

    @Column(name = "operador_id")
    private UUID operadorId;

    @Column(name = "habilitado_en_catalogo")
    private boolean habilitadoEnCatalogo;

    @Column(name = "version_habilitacion")
    private long versionHabilitacion;

    @Column(name = "fecha_registro")
    private Instant fechaRegistro;

    @Version
    private Long revision;

    protected PrestadorEntity() {
    }

    PrestadorEntity(UUID id, Instant fechaRegistro) {
        this.id = id;
        this.fechaRegistro = fechaRegistro;
    }

    void copiar(String numeroRnt, String tipo, Integer numeroEmpleados, String modalidades, String idiomas,
                String razonSocial, String municipio, String categoriaRnt, boolean personaNaturalRnt, String estadoRnt,
                boolean vistoEnRnt, Instant ultimaSincronizacion, String whatsappCifrado, boolean whatsappVerificado,
                String codigoHash, Instant codigoEmitido, int codigoIntentos, UUID operadorId,
                boolean habilitadoEnCatalogo, long versionHabilitacion) {
        this.numeroRnt = numeroRnt;
        this.tipo = tipo;
        this.numeroEmpleados = numeroEmpleados;
        this.modalidades = modalidades;
        this.idiomas = idiomas;
        this.razonSocial = razonSocial;
        this.municipio = municipio;
        this.categoriaRnt = categoriaRnt;
        this.personaNaturalRnt = personaNaturalRnt;
        this.estadoRnt = estadoRnt;
        this.vistoEnRnt = vistoEnRnt;
        this.ultimaSincronizacion = ultimaSincronizacion;
        this.whatsappCifrado = whatsappCifrado;
        this.whatsappVerificado = whatsappVerificado;
        this.codigoHash = codigoHash;
        this.codigoEmitido = codigoEmitido;
        this.codigoIntentos = codigoIntentos;
        this.operadorId = operadorId;
        this.habilitadoEnCatalogo = habilitadoEnCatalogo;
        this.versionHabilitacion = versionHabilitacion;
    }

    UUID id() {
        return id;
    }

    String numeroRnt() {
        return numeroRnt;
    }

    String tipo() {
        return tipo;
    }

    Integer numeroEmpleados() {
        return numeroEmpleados;
    }

    String modalidades() {
        return modalidades;
    }

    String idiomas() {
        return idiomas;
    }

    String razonSocial() {
        return razonSocial;
    }

    String municipio() {
        return municipio;
    }

    String categoriaRnt() {
        return categoriaRnt;
    }

    boolean personaNaturalRnt() {
        return personaNaturalRnt;
    }

    String estadoRnt() {
        return estadoRnt;
    }

    boolean vistoEnRnt() {
        return vistoEnRnt;
    }

    Instant ultimaSincronizacion() {
        return ultimaSincronizacion;
    }

    String whatsappCifrado() {
        return whatsappCifrado;
    }

    boolean whatsappVerificado() {
        return whatsappVerificado;
    }

    String codigoHash() {
        return codigoHash;
    }

    Instant codigoEmitido() {
        return codigoEmitido;
    }

    int codigoIntentos() {
        return codigoIntentos;
    }

    UUID operadorId() {
        return operadorId;
    }

    boolean habilitadoEnCatalogo() {
        return habilitadoEnCatalogo;
    }

    long versionHabilitacion() {
        return versionHabilitacion;
    }

    Instant fechaRegistro() {
        return fechaRegistro;
    }

    Long revision() {
        return revision;
    }
}
