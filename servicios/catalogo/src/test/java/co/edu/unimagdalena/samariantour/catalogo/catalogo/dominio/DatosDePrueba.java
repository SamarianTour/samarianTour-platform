package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Datos válidos para las pruebas del dominio y de la aplicación. */
public final class DatosDePrueba {

    public static final UUID PRESTADOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID OTRO_PRESTADOR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final Instant AHORA = Instant.parse("2026-10-09T15:00:00Z");
    public static final Instant DESPUES = AHORA.plusSeconds(3600);
    public static final String ZONA_ACTIVA = "parque-tayrona";
    public static final String ZONA_INACTIVA = "zona-cerrada";
    /** Foto que el PRESTADOR subió y que usan las ofertas de prueba; la aplicación la registra como suya. */
    public static final Foto FOTO = new Foto("3f2504e0-4f89-11d3-9a0c-0305e82c3301");
    /** Foto que subió el OTRO_PRESTADOR para sus ofertas. */
    public static final Foto FOTO_OTRO_PRESTADOR = new Foto("3f2504e0-4f89-11d3-9a0c-0305e82c3302");

    private DatosDePrueba() {
    }

    public static Zona zonaActiva() {
        return new Zona(ZONA_ACTIVA, "Parque Tayrona", "Parque Nacional Natural", new Coordenada(11.30, -74.07),
                true, true, true, List.of("No dejar residuos"), "Ingreso hasta las 16:00");
    }

    public static Zona zonaInactiva() {
        return new Zona(ZONA_INACTIVA, "Zona cerrada", null, null, false, false, false, List.of(), null);
    }

    public static ContenidoComun contenido(String nombre) {
        return contenido(nombre, ZONA_ACTIVA);
    }

    public static ContenidoComun contenido(String nombre, String zonaId) {
        return contenido(nombre, zonaId, FOTO);
    }

    public static ContenidoComun contenido(String nombre, String zonaId, Foto foto) {
        return new ContenidoComun(nombre, "Recorrido guiado por senderos del parque", new BigDecimal("120000"),
                "Martes a domingo", zonaId, new Coordenada(11.31, -74.06), "Desde la entrada El Zaino", 12,
                List.of(foto));
    }

    public static VersionActividad actividad() {
        return actividad("Caminata a Cabo San Juan");
    }

    public static VersionActividad actividad(String nombre) {
        return new VersionActividad(contenido(nombre), TipoActividad.SENDERISMO, NivelEsfuerzo.MODERADO, false,
                List.of(LocalTime.of(7, 0)), null, null, null, List.of());
    }

    public static VersionActividad actividadCompletaDeGuia() {
        return new VersionActividad(contenido("Avistamiento de aves"), TipoActividad.AVISTAMIENTO_FAUNA,
                NivelEsfuerzo.BAJO, true, List.of(LocalTime.of(6, 0)), "Parque de Minca", 180,
                ModalidadRecorrido.CAMINATA, List.of("es", "en"));
    }

    public static VersionAlojamiento alojamiento() {
        return new VersionAlojamiento(contenido("Cabaña frente al mar"), "cabaña", false);
    }

    public static VersionRestaurante restaurante() {
        return new VersionRestaurante(contenido("Cocina del Caribe"), true);
    }

    /** Paquete que incluye las ofertas de actividad con esos identificadores. */
    public static VersionPaquete paquete(String... actividadIds) {
        return new VersionPaquete(contenido("Tayrona en dos días"), List.of(actividadIds), 600,
                List.of(LocalTime.of(6, 30)));
    }

    public static PrestadorCatalogo habilitado() {
        return new PrestadorCatalogo(PRESTADOR, true, 1);
    }

    public static PrestadorCatalogo inhabilitado() {
        return new PrestadorCatalogo(PRESTADOR, false, 2);
    }

    /** Oferta de actividad de una agencia, con su primera versión en moderación. */
    public static Oferta ofertaEnModeracion() {
        return Oferta.publicar("oferta-1", habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad(),
                zonaActiva(), AHORA);
    }

    /** Oferta de actividad con su versión 1 aprobada y sin versión pendiente. */
    public static Oferta ofertaPublicada() {
        Oferta oferta = ofertaEnModeracion();
        oferta.aprobar(1, habilitado(), AHORA);
        return oferta;
    }

    /** Oferta de actividad de la agencia de prueba, ya aprobada, con el nivel de esfuerzo y la accesibilidad dados. */
    public static Oferta actividadPublicada(String id, NivelEsfuerzo nivelEsfuerzo, boolean accesibleSillaRuedas) {
        return actividadPublicada(id, habilitado(), nivelEsfuerzo, accesibleSillaRuedas);
    }

    public static Oferta actividadPublicada(String id, PrestadorCatalogo prestador, NivelEsfuerzo nivelEsfuerzo,
                                            boolean accesibleSillaRuedas) {
        VersionActividad contenido = new VersionActividad(contenido("Actividad " + id), TipoActividad.SENDERISMO,
                nivelEsfuerzo, accesibleSillaRuedas, List.of(LocalTime.of(7, 0)), null, null, null, List.of());
        Oferta oferta = Oferta.publicar(id, prestador, TipoPrestador.AGENCIA_OPERADORA, contenido, zonaActiva(), AHORA);
        oferta.aprobar(1, prestador, AHORA);
        return oferta;
    }
}
