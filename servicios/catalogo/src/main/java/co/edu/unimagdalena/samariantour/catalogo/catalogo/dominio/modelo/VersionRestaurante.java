package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

/**
 * Versión de una oferta de tipo restaurante.
 */
public final class VersionRestaurante extends VersionOferta {

    private final boolean accesibleSillaRuedas;

    /** Crea un borrador, aún sin enviar a moderación. */
    public VersionRestaurante(ContenidoComun contenido, boolean accesibleSillaRuedas) {
        this(contenido, accesibleSillaRuedas, null);
    }

    private VersionRestaurante(ContenidoComun contenido, boolean accesibleSillaRuedas,
                               RegistroModeracion moderacion) {
        super(contenido, moderacion);
        this.accesibleSillaRuedas = accesibleSillaRuedas;
    }

    /** Reconstruye una versión ya enviada; solo para los adaptadores de persistencia. */
    public static VersionRestaurante reconstituir(ContenidoComun contenido, boolean accesibleSillaRuedas,
                                                  RegistroModeracion moderacion) {
        return new VersionRestaurante(contenido, accesibleSillaRuedas,
                Validaciones.presente(moderacion, "moderacion"));
    }

    @Override
    public TipoOferta tipo() {
        return TipoOferta.RESTAURANTE;
    }

    @Override
    public String unidadPrecio() {
        return "promedio por persona";
    }

    public boolean esAccesibleSillaRuedas() {
        return accesibleSillaRuedas;
    }
}
