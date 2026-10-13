package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.time.Instant;
import java.util.Objects;

/**
 * Contenido de una oferta en un momento dado, sujeto a moderación (RF-66). La versión publicada es la que ve
 * el turista; la pendiente es la edición en revisión. Toda versión es de un tipo concreto: agregar un tipo de oferta
 * es agregar una subclase a {@code permits}.
 *
 * <p>Una versión nace como borrador, sin registro de moderación; la oferta le asigna número al enviarla.
 *
 * <p>La accesibilidad en silla de ruedas (RF-17) no es un método común: actividad, alojamiento y restaurante
 * la guardan, y la del paquete se calcula con sus actividades en ReglasPaquete.
 */
public abstract sealed class VersionOferta
        permits VersionActividad, VersionPaquete, VersionAlojamiento, VersionRestaurante {

    private final ContenidoComun contenido;
    private RegistroModeracion moderacion;

    protected VersionOferta(ContenidoComun contenido, RegistroModeracion moderacion) {
        this.contenido = Validaciones.presente(contenido, "contenido");
        this.moderacion = moderacion;
    }

    public abstract TipoOferta tipo();

    /** Unidad del precio de referencia (RF-20). */
    public abstract String unidadPrecio();

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

    /**
     * Copia de este borrador enviada a moderación con el número indicado. El borrador no cambia, así que el caso de
     * uso que lo envía puede repetirse si otra escritura se cruza con él (ADR-05).
     */
    VersionOferta enviada(int numero, Instant ahora) {
        if (moderacion != null) {
            throw new IllegalStateException("La versión ya fue enviada a moderación");
        }
        return conModeracion(RegistroModeracion.enviada(numero, Objects.requireNonNull(ahora)));
    }

    /** Copia de esta versión con otro registro de moderación; cada tipo crea la suya. */
    abstract VersionOferta conModeracion(RegistroModeracion moderacion);

    void aprobar(Instant ahora) {
        moderacion = moderacion().aprobada(ahora);
    }

    void rechazar(String motivo, Instant ahora) {
        moderacion = moderacion().rechazada(motivo, ahora);
    }
}
