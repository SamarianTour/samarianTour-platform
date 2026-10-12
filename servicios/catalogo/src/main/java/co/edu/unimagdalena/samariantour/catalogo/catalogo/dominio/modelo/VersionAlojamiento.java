package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

/**
 * Versión de una oferta de tipo alojamiento.
 *
 * <p>tipoUnidad (habitación, cabaña, apartamento) queda opcional hasta confirmarlo con el SRS.
 */
public final class VersionAlojamiento extends VersionOferta {

    private final String tipoUnidad;
    private final boolean accesibleSillaRuedas;

    /** Crea un borrador, aún sin enviar a moderación. */
    public VersionAlojamiento(ContenidoComun contenido, String tipoUnidad, boolean accesibleSillaRuedas) {
        this(contenido, tipoUnidad, accesibleSillaRuedas, null);
    }

    private VersionAlojamiento(ContenidoComun contenido, String tipoUnidad, boolean accesibleSillaRuedas,
                               RegistroModeracion moderacion) {
        super(contenido, moderacion);
        this.tipoUnidad = Validaciones.textoOpcional(tipoUnidad);
        this.accesibleSillaRuedas = accesibleSillaRuedas;
    }

    /** Reconstruye una versión ya enviada; solo para los adaptadores de persistencia. */
    public static VersionAlojamiento reconstituir(ContenidoComun contenido, String tipoUnidad,
                                                  boolean accesibleSillaRuedas, RegistroModeracion moderacion) {
        return new VersionAlojamiento(contenido, tipoUnidad, accesibleSillaRuedas,
                Validaciones.presente(moderacion, "moderacion"));
    }

    @Override
    public TipoOferta tipo() {
        return TipoOferta.ALOJAMIENTO;
    }

    @Override
    public String unidadPrecio() {
        return "por noche y unidad";
    }

    public boolean esAccesibleSillaRuedas() {
        return accesibleSillaRuedas;
    }

    public String tipoUnidad() {
        return tipoUnidad;
    }
}
