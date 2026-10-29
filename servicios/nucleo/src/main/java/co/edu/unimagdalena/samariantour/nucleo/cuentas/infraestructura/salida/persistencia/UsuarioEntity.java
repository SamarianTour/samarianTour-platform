package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Una fila de {@code usuarios} (V8): los cinco roles en una sola tabla. El nombre y el WhatsApp llegan aquí ya cifrados
 * (DN-28). La revisión es la versión de JPA, para el bloqueo optimista (ADR-05).
 */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    private UUID id;

    private String rol;

    @Column(name = "nombre_cifrado")
    private String nombreCifrado;

    private String correo;

    @Column(name = "hash_contrasena")
    private String hashContrasena;

    private String estado;

    @Column(name = "intentos_fallidos")
    private int intentosFallidos;

    @Column(name = "bloqueada_hasta")
    private Instant bloqueadaHasta;

    @Column(name = "fecha_registro")
    private Instant fechaRegistro;

    @Column(name = "whatsapp_cifrado")
    private String whatsappCifrado;

    @Column(name = "whatsapp_verificado")
    private boolean whatsappVerificado;

    @Version
    private Long revision;

    protected UsuarioEntity() {
    }

    UsuarioEntity(UUID id, String rol, Instant fechaRegistro) {
        this.id = id;
        this.rol = rol;
        this.fechaRegistro = fechaRegistro;
    }

    void copiar(String nombreCifrado, String correo, String hashContrasena, String estado, int intentosFallidos,
                Instant bloqueadaHasta, String whatsappCifrado, boolean whatsappVerificado) {
        this.nombreCifrado = nombreCifrado;
        this.correo = correo;
        this.hashContrasena = hashContrasena;
        this.estado = estado;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadaHasta = bloqueadaHasta;
        this.whatsappCifrado = whatsappCifrado;
        this.whatsappVerificado = whatsappVerificado;
    }

    UUID id() {
        return id;
    }

    String rol() {
        return rol;
    }

    String nombreCifrado() {
        return nombreCifrado;
    }

    String correo() {
        return correo;
    }

    String hashContrasena() {
        return hashContrasena;
    }

    String estado() {
        return estado;
    }

    int intentosFallidos() {
        return intentosFallidos;
    }

    Instant bloqueadaHasta() {
        return bloqueadaHasta;
    }

    Instant fechaRegistro() {
        return fechaRegistro;
    }

    String whatsappCifrado() {
        return whatsappCifrado;
    }

    boolean whatsappVerificado() {
        return whatsappVerificado;
    }

    Long revision() {
        return revision;
    }
}
