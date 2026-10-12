package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * Réplica en el catálogo de la habilitación RNT de un prestador, alimentada por los avisos del núcleo (RF-77).
 * Es la fuente de verdad del catálogo para autorizar publicaciones; las ofertas guardan una copia para la búsqueda.
 * El tipo de prestador no se guarda aquí: llega en el token de acceso.
 *
 * <p>Pendiente (RF-69): si se aplica la regla de modalidades del guía, esta réplica necesitará las modalidades declaradas.
 */
public final class PrestadorCatalogo {

    private final UUID prestadorId;
    private boolean rntActivo;
    private long version;

    /**
     * @param version versión del prestador en el núcleo; crece con cada cambio y ordena los avisos aunque lleguen en desorden
     */
    public PrestadorCatalogo(UUID prestadorId, boolean rntActivo, long version) {
        this.prestadorId = Objects.requireNonNull(prestadorId, "prestadorId");
        this.rntActivo = rntActivo;
        this.version = version;
    }

    /** Un prestador del que aún no llega ningún aviso se trata como no habilitado. */
    public static PrestadorCatalogo sinAvisos(UUID prestadorId) {
        return new PrestadorCatalogo(prestadorId, false, 0);
    }

    /**
     * Aplica un aviso de habilitación del núcleo. Descarta los avisos con una versión igual o anterior a la conocida.
     *
     * @return true si el aviso cambió la réplica
     */
    public boolean aplicarHabilitacion(boolean rntActivo, long version) {
        if (version <= this.version) {
            return false;
        }
        this.rntActivo = rntActivo;
        this.version = version;
        return true;
    }

    public UUID prestadorId() {
        return prestadorId;
    }

    public boolean rntActivo() {
        return rntActivo;
    }

    public long version() {
        return version;
    }
}
