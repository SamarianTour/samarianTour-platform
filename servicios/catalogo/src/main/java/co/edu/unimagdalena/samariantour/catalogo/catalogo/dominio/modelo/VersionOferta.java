package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.time.Instant;
import java.util.Objects;

/**
 * Contenido de una oferta en un momento dado, sujeto a moderación (RF-66). La versión publicada es la que ve
 * el turista; la pendiente es la edición en revisión. Toda versión es de un tipo concreto: agregar un tipo de oferta
 * es agregar una subclase a {@code permits}.
 *
 * <p>Una versión nace como borrador, sin registro de moderación; la oferta le asigna número al enviarla.
 */
public abstract sealed class VersionOferta permits VersionActividad, VersionAlojamiento {

    private final ContenidoComun contenido;
    private RegistroModeracion moderacion;

    protected VersionOferta(ContenidoComun contenido, RegistroModeracion moderacion) {
        this.contenido = Validaciones.presente(contenido, "contenido");
        this.moderacion = moderacion;
    }

    public abstract TipoOferta tipo();

    /** Unidad del precio de referencia (RF-20). */
    public abstract String unidadPrecio();

    public abstract boolean esAccesibleSillaRuedas();

    public ContenidoComun contenido() {
        return contenido;
    }

    public boolean estaEnviada() {
        return moderacion != null;
    }

    public RegistroModeracion moderacion() {
        if (moderacion == null) {
            throw new IllegalStateException("La versión todavía es un borrador");
        }
        return moderacion;
    }

    public int numero() {
        return moderacion().numero();
    }

    public EstadoModeracion estadoModeracion() {
        return moderacion().estado();
    }

    void enviar(int numero, Instant ahora) {
        if (moderacion != null) {
            throw new IllegalStateException("La versión ya fue enviada a moderación");
        }
        moderacion = RegistroModeracion.enviada(numero, Objects.requireNonNull(ahora));
    }

    void aprobar(Instant ahora) {
        moderacion = moderacion().aprobada(ahora);
    }

    void rechazar(String motivo, Instant ahora) {
        moderacion = moderacion().rechazada(motivo, ahora);
    }
}
