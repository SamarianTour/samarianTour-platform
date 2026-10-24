package co.edu.unimagdalena.samariantour.recomendacion.recomendacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Ofertas y preferencias para las pruebas. Por defecto, una caminata moderada en Minca, accesible, de 80.000 pesos.
 */
public final class OfertasDePrueba {

    public static final String MINCA = "a00000000000000000000001";
    public static final String TAGANGA = "a00000000000000000000002";

    private OfertasDePrueba() {
    }

    public static Oferta oferta(String id) {
        return new Oferta(id);
    }

    public static Preferencia preferencias() {
        return new Preferencia();
    }

    /** Constructor de ofertas candidatas con valores por defecto. */
    public static final class Oferta {
        private final String id;
        private TipoOferta tipo = TipoOferta.ACTIVIDAD;
        private String nombre;
        private String descripcion = "Recorrido guiado por la Sierra Nevada";
        private UUID prestadorId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        private String zonaId = MINCA;
        private String zonaNombre = "Minca";
        private BigDecimal precio = new BigDecimal("80000");
        private Set<TipoActividad> tiposActividad = Set.of(TipoActividad.SENDERISMO);
        private NivelEsfuerzo nivel = NivelEsfuerzo.MODERADO;
        private boolean accesible = true;

        private Oferta(String id) {
            this.id = id;
            this.nombre = "Oferta " + id;
        }

        public Oferta tipo(TipoOferta valor) {
            tipo = valor;
            return this;
        }

        public Oferta nombre(String valor) {
            nombre = valor;
            return this;
        }

        public Oferta descripcion(String valor) {
            descripcion = valor;
            return this;
        }

        public Oferta prestador(UUID valor) {
            prestadorId = valor;
            return this;
        }

        public Oferta zona(String id, String nombreZona) {
            zonaId = id;
            zonaNombre = nombreZona;
            return this;
        }

        public Oferta precio(long valor) {
            precio = BigDecimal.valueOf(valor);
            return this;
        }

        public Oferta actividades(TipoActividad... valores) {
            tiposActividad = Set.of(valores);
            return this;
        }

        public Oferta nivel(NivelEsfuerzo valor) {
            nivel = valor;
            return this;
        }

        public Oferta accesible(boolean valor) {
            accesible = valor;
            return this;
        }

        public OfertaCandidata crear() {
            return new OfertaCandidata(id, tipo, nombre, descripcion, prestadorId, zonaId, zonaNombre, precio,
                    "por persona", tiposActividad, nivel, accesible);
        }
    }

    /** Constructor de preferencias; sin nada, falla como en la API. */
    public static final class Preferencia {
        private String texto;
        private Idioma idioma = Idioma.ES;
        private Set<TipoOferta> tiposOferta = Set.of();
        private Set<TipoActividad> tiposActividad = Set.of();
        private String zonaId;
        private boolean accesible;
        private NivelEsfuerzo nivelMaximo;
        private BigDecimal precioMaximo;

        public Preferencia texto(String valor) {
            texto = valor;
            return this;
        }

        public Preferencia idioma(Idioma valor) {
            idioma = valor;
            return this;
        }

        public Preferencia tipos(TipoOferta... valores) {
            tiposOferta = Set.of(valores);
            return this;
        }

        public Preferencia actividades(TipoActividad... valores) {
            tiposActividad = Set.of(valores);
            return this;
        }

        public Preferencia zona(String valor) {
            zonaId = valor;
            return this;
        }

        public Preferencia accesible() {
            accesible = true;
            return this;
        }

        public Preferencia nivelMaximo(NivelEsfuerzo valor) {
            nivelMaximo = valor;
            return this;
        }

        public Preferencia precioMaximo(long valor) {
            precioMaximo = BigDecimal.valueOf(valor);
            return this;
        }

        public Preferencias crear() {
            return new Preferencias(texto, idioma, tiposOferta, tiposActividad, zonaId, accesible, nivelMaximo,
                    precioMaximo);
        }
    }
}
