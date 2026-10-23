package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Opiniones y habilitaciones de demostración sobre las ofertas de {@code DatosSantaMarta}: 9 aprobadas repartidas en
 * 4 ofertas visibles, 2 en moderación y 1 rechazada. Son inventadas, como el resto de los datos de la demostración.
 *
 * <p>Los turistas tienen UUID fijos porque los emite el núcleo (DC-30). Laura y Andrés son los turistas demo que se
 * eligen en el frontend; los demás solo existen para que las ofertas tengan varias opiniones. Cada opinión tiene su
 * habilitación (el turista pagó la oferta), y Laura además puede opinar sobre dos ofertas en las que aún no lo hizo.
 *
 * <p>Las ofertas se nombran por su id: este módulo no puede importar los datos del catálogo (DC-40).
 */
public final class DatosOpiniones {

    public static final UUID LAURA = UUID.fromString("70000000-0000-0000-0000-000000000007");
    public static final UUID ANDRES = UUID.fromString("80000000-0000-0000-0000-000000000008");
    public static final UUID TURISTA_1 = UUID.fromString("90000000-0000-0000-0000-000000000001");
    public static final UUID TURISTA_2 = UUID.fromString("90000000-0000-0000-0000-000000000002");
    public static final UUID TURISTA_3 = UUID.fromString("90000000-0000-0000-0000-000000000003");
    public static final UUID TURISTA_4 = UUID.fromString("90000000-0000-0000-0000-000000000004");
    public static final UUID TURISTA_5 = UUID.fromString("90000000-0000-0000-0000-000000000005");

    // Ofertas de DatosSantaMarta.
    public static final String CAMINATA_CABO_SAN_JUAN = "b00000000000000000000001";
    public static final String SNORKEL_PLAYA_CRISTAL = "b00000000000000000000002";
    public static final String KAYAK_BAHIA_TAGANGA = "b00000000000000000000004";
    public static final String RECORRIDO_CENTRO_HISTORICO = "b00000000000000000000006";
    public static final String PAQUETE_TAYRONA_MAR_Y_SELVA = "b00000000000000000000008";
    public static final String AVISTAMIENTO_AVES_MINCA = "b0000000000000000000000a";
    public static final String RUTA_CAFE_CACAO_MINCA = "b0000000000000000000000b";
    public static final String PESCADOS_FRENTE_AL_MAR_TAGANGA = "b00000000000000000000014";

    public static final String OPINION_LAURA_CABO_SAN_JUAN = "d00000000000000000000001";
    public static final String OPINION_EN_MODERACION_CAFE = "d0000000000000000000000a";
    public static final String OPINION_ANDRES_KAYAK = "d0000000000000000000000b";
    public static final String OPINION_RECHAZADA = "d0000000000000000000000c";

    private DatosOpiniones() {
    }

    public static List<Opinion> opiniones() {
        return List.of(
                aprobada(OPINION_LAURA_CABO_SAN_JUAN, CAMINATA_CABO_SAN_JUAN, LAURA, 5,
                        "Sendero exigente pero precioso. El guía explicó cómo cuidar el parque y paramos a tiempo "
                                + "para nadar en Cabo San Juan.",
                        "2026-09-14T22:10:00Z"),
                aprobada("d00000000000000000000002", CAMINATA_CABO_SAN_JUAN, TURISTA_1, 4,
                        "Muy buena caminata. Lleven agua suficiente: el tramo por la selva es largo y húmedo.",
                        "2026-09-20T18:45:00Z"),
                aprobada("d00000000000000000000003", CAMINATA_CABO_SAN_JUAN, TURISTA_2, 5,
                        "El mejor día del viaje. Salimos temprano y llegamos antes que los grupos grandes.",
                        "2026-10-02T23:05:00Z"),
                aprobada("d00000000000000000000004", PAQUETE_TAYRONA_MAR_Y_SELVA, ANDRES, 5,
                        "Dos días bien organizados: la caminata y el snorkel encajan, y el equipo estaba en buen "
                                + "estado.",
                        "2026-09-28T16:30:00Z"),
                aprobada("d00000000000000000000005", PAQUETE_TAYRONA_MAR_Y_SELVA, TURISTA_3, 4,
                        "Buen paquete. El regreso en lancha se retrasó un poco por el oleaje, pero nos avisaron.",
                        "2026-10-05T20:00:00Z"),
                aprobada("d00000000000000000000006", RUTA_CAFE_CACAO_MINCA, TURISTA_1, 5,
                        "La familia de la finca explicó todo el proceso del café. La degustación vale la pena.",
                        "2026-09-10T15:20:00Z"),
                aprobada("d00000000000000000000007", RUTA_CAFE_CACAO_MINCA, TURISTA_4, 4,
                        "Recorrido tranquilo y bien explicado. El camino tiene barro cuando llueve.",
                        "2026-09-25T14:00:00Z"),
                aprobada("d00000000000000000000008", PESCADOS_FRENTE_AL_MAR_TAGANGA, TURISTA_2, 4,
                        "Pescado fresco y bien preparado, con vista a la bahía. Se llena al atardecer.",
                        "2026-09-18T01:15:00Z"),
                aprobada("d00000000000000000000009", PESCADOS_FRENTE_AL_MAR_TAGANGA, TURISTA_5, 3,
                        "La comida estuvo bien, pero tardaron casi una hora en atendernos un sábado.",
                        "2026-10-06T00:40:00Z"),
                enviada(OPINION_EN_MODERACION_CAFE, RUTA_CAFE_CACAO_MINCA, TURISTA_3, 5,
                        "Aprendimos a tostar el café y nos llevamos una libra. Muy recomendado.",
                        "2026-10-18T17:00:00Z"),
                enviada(OPINION_ANDRES_KAYAK, KAYAK_BAHIA_TAGANGA, ANDRES, 4,
                        "Kayaks en buen estado y el instructor estuvo pendiente de todos en el agua.",
                        "2026-10-19T13:30:00Z"),
                rechazada(OPINION_RECHAZADA, CAMINATA_CABO_SAN_JUAN, TURISTA_4, 2,
                        "Para conseguir este tour más barato escríbanme al WhatsApp de mi agencia.",
                        "2026-10-08T19:00:00Z", "Incluye publicidad y datos de contacto"));
    }

    /** Una habilitación por opinión, más dos ofertas que Laura pagó y todavía no calificó. */
    public static List<HabilitacionOpinion> habilitaciones() {
        List<HabilitacionOpinion> habilitaciones = new ArrayList<>(opiniones().stream()
                .map(opinion -> new HabilitacionOpinion(opinion.turistaId(), opinion.ofertaId()))
                .toList());
        habilitaciones.add(new HabilitacionOpinion(LAURA, SNORKEL_PLAYA_CRISTAL));
        habilitaciones.add(new HabilitacionOpinion(LAURA, AVISTAMIENTO_AVES_MINCA));
        return List.copyOf(habilitaciones);
    }

    private static Opinion enviada(String id, String ofertaId, UUID turistaId, int calificacion, String comentario,
                                   String fecha) {
        return Opinion.enviar(id, ofertaId, turistaId, calificacion, comentario, Instant.parse(fecha));
    }

    /** Aprobada un día después de enviada. */
    private static Opinion aprobada(String id, String ofertaId, UUID turistaId, int calificacion, String comentario,
                                    String fecha) {
        Opinion opinion = enviada(id, ofertaId, turistaId, calificacion, comentario, fecha);
        opinion.aprobar(Instant.parse(fecha).plusSeconds(86_400));
        return opinion;
    }

    private static Opinion rechazada(String id, String ofertaId, UUID turistaId, int calificacion, String comentario,
                                     String fecha, String motivo) {
        Opinion opinion = enviada(id, ofertaId, turistaId, calificacion, comentario, fecha);
        opinion.rechazar(motivo, Instant.parse(fecha).plusSeconds(86_400));
        return opinion;
    }
}
