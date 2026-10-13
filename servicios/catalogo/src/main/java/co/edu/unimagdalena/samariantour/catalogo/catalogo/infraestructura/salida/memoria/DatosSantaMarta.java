package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Datos de demostración del prototipo: las zonas de la precarga (DC-18), cinco prestadores y sus ofertas.
 * Son inventados para la demostración y no describen prestadores reales; las coordenadas de las zonas son
 * aproximadas y sus marcas son un supuesto hasta validarlas con una fuente oficial.
 *
 * <p>Los identificadores son fijos para que todas las réplicas arranquen con los mismos datos. Las ofertas
 * se construyen con las operaciones del dominio (publicar, aprobar, editar), así que cumplen sus reglas.
 */
final class DatosSantaMarta {

    static final UUID AGENCIA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID GUIA = UUID.fromString("20000000-0000-0000-0000-000000000002");
    static final UUID HOTEL = UUID.fromString("30000000-0000-0000-0000-000000000003");
    static final UUID RESTAURANTE = UUID.fromString("40000000-0000-0000-0000-000000000004");
    /** Agencia a la que el núcleo le retiró la habilitación: sus ofertas no son visibles (RF-77). */
    static final UUID AGENCIA_SIN_RNT = UUID.fromString("50000000-0000-0000-0000-000000000005");

    static final String CENTRO_HISTORICO = "a00000000000000000000001";
    static final String RODADERO = "a00000000000000000000002";
    static final String TAGANGA = "a00000000000000000000003";
    static final String ZONA_SUR = "a00000000000000000000004";
    static final String TAYRONA = "a00000000000000000000005";
    static final String TRONCAL = "a00000000000000000000006";
    static final String MINCA = "a00000000000000000000007";

    // Identificadores fijos con formato ObjectId (DC-30): iguales en memoria y en la precarga de MongoDB.
    static final String CAMINATA_CABO_SAN_JUAN = "b00000000000000000000001";
    static final String SNORKEL_PLAYA_CRISTAL = "b00000000000000000000002";
    static final String BAUTIZO_BUCEO_TAGANGA = "b00000000000000000000003";
    static final String KAYAK_BAHIA_TAGANGA = "b00000000000000000000004";
    static final String TUBING_RIO_BURITACA = "b00000000000000000000005";
    static final String RECORRIDO_CENTRO_HISTORICO = "b00000000000000000000006";
    static final String ATARDECER_VELERO_BAHIA = "b00000000000000000000007";
    static final String PAQUETE_TAYRONA_MAR_Y_SELVA = "b00000000000000000000008";
    static final String PAQUETE_TAGANGA_BAJO_EL_AGUA = "b00000000000000000000009";
    static final String AVISTAMIENTO_AVES_MINCA = "b0000000000000000000000a";
    static final String RUTA_CAFE_CACAO_MINCA = "b0000000000000000000000b";
    static final String CAMINATA_POZO_AZUL_MARINKA = "b0000000000000000000000c";
    static final String DESCENSO_BICICLETA_SIERRA = "b0000000000000000000000d";
    static final String PAQUETE_MINCA_AVES_Y_CAFE = "b0000000000000000000000e";
    static final String CABANA_FRENTE_AL_MAR_BURITACA = "b0000000000000000000000f";
    static final String HABITACION_DOBLE_RODADERO = "b00000000000000000000010";
    static final String APARTAMENTO_POZOS_COLORADOS = "b00000000000000000000011";
    static final String HABITACION_ECOLOGICA_MINCA = "b00000000000000000000012";
    static final String COCINA_CARIBE_CENTRO_HISTORICO = "b00000000000000000000013";
    static final String PESCADOS_FRENTE_AL_MAR_TAGANGA = "b00000000000000000000014";
    static final String PARRILLA_RODADERO = "b00000000000000000000015";
    static final String EXPEDICION_CIUDAD_PERDIDA = "b00000000000000000000016";
    static final String PASEO_LANCHA_PLAYA_BLANCA = "b00000000000000000000017";

    private static final Instant CARGA = Instant.parse("2026-10-01T13:00:00Z");

    private DatosSantaMarta() {
    }

    static List<Zona> zonas() {
        List<String> practicasAreaProtegida = List.of("No dejar residuos", "No extraer fauna ni flora",
                "Permanecer en los senderos señalizados");
        String condicionesAreaProtegida = "Consulta los horarios de ingreso y los cierres temporales antes de tu visita";
        return List.of(
                new Zona(CENTRO_HISTORICO, "Centro Histórico", "Casco antiguo, catedral, malecón y marina",
                        new Coordenada(11.2447, -74.2110), false, false, true, List.of(), null),
                // TODO: definir las marcas de El Rodadero y Gaira (DC-18 las deja sin definir).
                new Zona(RODADERO, "El Rodadero y Gaira", "Playas urbanas y salida de lanchas hacia Playa Blanca",
                        new Coordenada(11.2016, -74.2280), false, false, true, List.of(), null),
                new Zona(TAGANGA, "Taganga", "Bahía de pescadores y punto de salida de buceo",
                        new Coordenada(11.2675, -74.1900), false, true, true, List.of(), null),
                // TODO: definir las marcas de la zona sur hotelera (DC-18 las deja sin definir).
                new Zona(ZONA_SUR, "Zona sur hotelera", "Pozos Colorados, Bello Horizonte y Playa Salguero",
                        new Coordenada(11.1560, -74.2270), false, false, true, List.of(), null),
                new Zona(TAYRONA, "Parque Tayrona", "Parque Nacional Natural con playas, selva y senderos",
                        new Coordenada(11.3060, -74.0650), true, true, true, practicasAreaProtegida,
                        condicionesAreaProtegida),
                // TODO: definir las marcas de la Troncal del Caribe (DC-18 las deja sin definir).
                new Zona(TRONCAL, "Troncal del Caribe", "Guachaca, Buritaca y Don Diego",
                        new Coordenada(11.2570, -73.7700), false, false, true, List.of(), null),
                new Zona(MINCA, "Minca y Sierra Nevada", "Bonda, Minca y Ciudad Perdida",
                        new Coordenada(11.1430, -74.1170), true, false, true, practicasAreaProtegida,
                        condicionesAreaProtegida));
    }

    static List<PrestadorCatalogo> prestadores() {
        return List.of(habilitado(AGENCIA), habilitado(GUIA), habilitado(HOTEL), habilitado(RESTAURANTE),
                new PrestadorCatalogo(AGENCIA_SIN_RNT, false, 2));
    }

    static List<PerfilPublico> perfiles() {
        return List.of(
                new PerfilPublico(AGENCIA, "Agencia operadora local con salidas diarias al Tayrona y a Taganga",
                        List.of("https://www.example.com/fotos/agencia-1.jpg",
                                "https://www.example.com/fotos/agencia-2.jpg"),
                        "Lunes a sábado, de 7:00 a 18:00", "https://www.example.com/agencia",
                        List.of("https://www.example.com/redes/agencia")),
                // El guía solo completó la descripción: los demás campos del perfil son opcionales (DC-26).
                new PerfilPublico(GUIA, "Guía independiente de Minca, especializado en aves y cultura cafetera",
                        List.of(), null, null, List.of()),
                new PerfilPublico(HOTEL, "Alojamientos familiares en la costa y en la Sierra",
                        List.of("https://www.example.com/fotos/alojamiento-1.jpg"), "Recepción las 24 horas",
                        "https://www.example.com/alojamientos", List.of()),
                new PerfilPublico(RESTAURANTE, "Cocina del Caribe con pesca del día",
                        List.of("https://www.example.com/fotos/restaurante-1.jpg"),
                        "Todos los días, de 12:00 a 22:00", null,
                        List.of("https://www.example.com/redes/restaurante")));
    }

    static List<Oferta> ofertas() {
        Map<String, Zona> zonas = zonas().stream().collect(Collectors.toMap(Zona::id, Function.identity()));
        List<Oferta> ofertas = new ArrayList<>();

        // Actividades de la agencia
        Oferta caminataCaboSanJuan = publicada(zonas, CAMINATA_CABO_SAN_JUAN, AGENCIA,
                TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Caminata a Cabo San Juan del Guía",
                                "Sendero por la selva y las playas del parque hasta Cabo San Juan", "150000",
                                "Martes a domingo", TAYRONA, 12),
                        TipoActividad.SENDERISMO, NivelEsfuerzo.MODERADO, false, horas(7, 8), 420));
        Oferta snorkelPlayaCristal = publicada(zonas, SNORKEL_PLAYA_CRISTAL, AGENCIA,
                TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Snorkel en Playa Cristal",
                                "Traslado en lancha y careteo guiado sobre el arrecife", "180000",
                                "Todos los días", TAYRONA, 10),
                        TipoActividad.BUCEO_SNORKEL, NivelEsfuerzo.BAJO, false, horas(8), 360));
        Oferta bautizoBuceo = publicada(zonas, BAUTIZO_BUCEO_TAGANGA, AGENCIA, TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Bautizo de buceo en Taganga",
                                "Primera inmersión con instructor, sin experiencia previa", "320000",
                                "Todos los días", TAGANGA, 6),
                        TipoActividad.BUCEO_SNORKEL, NivelEsfuerzo.MODERADO, false, horas(8, 13), 240));
        Oferta kayakTaganga = publicada(zonas, KAYAK_BAHIA_TAGANGA, AGENCIA, TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Kayak en la bahía de Taganga",
                                "Recorrido en kayak doble por la bahía con parada para nadar", "110000",
                                "Todos los días", TAGANGA, 8),
                        TipoActividad.DEPORTES_ACUATICOS, NivelEsfuerzo.MODERADO, false, horas(7, 15), 150));
        Oferta tubingBuritaca = publicada(zonas, TUBING_RIO_BURITACA, AGENCIA, TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Tubing por el río Buritaca",
                                "Descenso en neumático por el río hasta su desembocadura en el mar", "90000",
                                "Todos los días", TRONCAL, 15),
                        TipoActividad.AVENTURA, NivelEsfuerzo.BAJO, false, horas(9, 14), 180));
        Oferta recorridoCentro = publicada(zonas, RECORRIDO_CENTRO_HISTORICO, AGENCIA,
                TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Recorrido a pie por el Centro Histórico",
                                "Caminata guiada por la catedral, las plazas y el malecón", "60000",
                                "Lunes a sábado", CENTRO_HISTORICO, 20),
                        TipoActividad.CULTURAL_HISTORICO, NivelEsfuerzo.BAJO, true, horas(9, 16), 120));
        Oferta veleroBahia = publicada(zonas, ATARDECER_VELERO_BAHIA, AGENCIA, TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Atardecer en velero por la bahía",
                                "Navegación al atardecer con salida desde la marina", "220000",
                                "Jueves a domingo", CENTRO_HISTORICO, 8),
                        TipoActividad.NAUTICO, NivelEsfuerzo.BAJO, false, horas(16), 150));
        ofertas.addAll(List.of(caminataCaboSanJuan, snorkelPlayaCristal, bautizoBuceo, kayakTaganga, tubingBuritaca,
                recorridoCentro, veleroBahia));

        // Paquetes de la agencia
        ofertas.add(publicada(zonas, PAQUETE_TAYRONA_MAR_Y_SELVA, AGENCIA, TipoPrestador.AGENCIA_OPERADORA,
                new VersionPaquete(contenido("Tayrona: mar y selva",
                        "Dos días en el parque: caminata a Cabo San Juan y snorkel en Playa Cristal", "300000",
                        "Salidas los martes y los viernes", TAYRONA, 10),
                        List.of(caminataCaboSanJuan.id(), snorkelPlayaCristal.id()), 780, horas(7)),
                caminataCaboSanJuan, snorkelPlayaCristal));
        ofertas.add(publicada(zonas, PAQUETE_TAGANGA_BAJO_EL_AGUA, AGENCIA, TipoPrestador.AGENCIA_OPERADORA,
                new VersionPaquete(contenido("Taganga bajo el agua",
                        "Kayak por la bahía en la mañana y bautizo de buceo en la tarde", "390000",
                        "Todos los días", TAGANGA, 6),
                        List.of(kayakTaganga.id(), bautizoBuceo.id()), 420, horas(7)),
                kayakTaganga, bautizoBuceo));

        // Actividades del guía independiente: exigen punto de encuentro, duración, modalidad e idiomas (RF-67)
        Oferta avesMinca = publicada(zonas, AVISTAMIENTO_AVES_MINCA, GUIA, TipoPrestador.GUIA_INDEPENDIENTE,
                actividadDeGuia(contenido("Avistamiento de aves en Minca",
                                "Recorrido al amanecer para observar aves de la Sierra", "120000",
                                "Todos los días", MINCA, 6),
                        TipoActividad.AVISTAMIENTO_FAUNA, NivelEsfuerzo.BAJO, horas(6), "Plaza principal de Minca",
                        240, ModalidadRecorrido.CAMINATA, List.of("es", "en")));
        Oferta cafeCacao = publicada(zonas, RUTA_CAFE_CACAO_MINCA, GUIA, TipoPrestador.GUIA_INDEPENDIENTE,
                actividadDeGuia(contenido("Ruta del café y el cacao",
                                "Visita a fincas de la vereda con degustación", "85000",
                                "Lunes a sábado", MINCA, 10),
                        TipoActividad.GASTRONOMICO, NivelEsfuerzo.BAJO, horas(9, 14), "Plaza principal de Minca",
                        180, ModalidadRecorrido.CAMINATA, List.of("es", "en", "fr")));
        ofertas.addAll(List.of(avesMinca, cafeCacao));
        ofertas.add(publicada(zonas, CAMINATA_POZO_AZUL_MARINKA, GUIA, TipoPrestador.GUIA_INDEPENDIENTE,
                actividadDeGuia(contenido("Caminata a Pozo Azul y cascada de Marinka",
                                "Sendero de montaña con baño en pozos y cascadas", "95000",
                                "Todos los días", MINCA, 8),
                        TipoActividad.SENDERISMO, NivelEsfuerzo.MODERADO, horas(8), "Plaza principal de Minca",
                        300, ModalidadRecorrido.CAMINATA, List.of("es"))));
        ofertas.add(publicada(zonas, DESCENSO_BICICLETA_SIERRA, GUIA, TipoPrestador.GUIA_INDEPENDIENTE,
                actividadDeGuia(contenido("Descenso en bicicleta desde la Sierra",
                                "Bajada por caminos destapados desde la parte alta hasta Minca", "160000",
                                "Miércoles a domingo", MINCA, 6),
                        TipoActividad.CICLISMO, NivelEsfuerzo.ALTA_EXIGENCIA, horas(7), "Plaza principal de Minca",
                        300, ModalidadRecorrido.BICICLETA, List.of("es", "en"))));

        // Paquete del guía
        ofertas.add(publicada(zonas, PAQUETE_MINCA_AVES_Y_CAFE, GUIA, TipoPrestador.GUIA_INDEPENDIENTE,
                new VersionPaquete(contenido("Minca: aves y café",
                        "Avistamiento de aves al amanecer y ruta del café y el cacao", "190000",
                        "Lunes a sábado", MINCA, 6),
                        List.of(avesMinca.id(), cafeCacao.id()), 480, horas(6)),
                avesMinca, cafeCacao));

        // Alojamientos
        ofertas.add(publicada(zonas, CABANA_FRENTE_AL_MAR_BURITACA, HOTEL, TipoPrestador.ALOJAMIENTO,
                new VersionAlojamiento(contenido("Cabaña frente al mar en Buritaca",
                        "Cabaña para cuatro personas entre el río y el mar", "380000", null, TRONCAL, 4),
                        "cabaña", false)));
        ofertas.add(publicada(zonas, HABITACION_DOBLE_RODADERO, HOTEL, TipoPrestador.ALOJAMIENTO,
                new VersionAlojamiento(contenido("Habitación doble en El Rodadero",
                        "Habitación con aire acondicionado a dos cuadras de la playa", "290000", null, RODADERO, 2),
                        "habitación", true)));
        ofertas.add(publicada(zonas, APARTAMENTO_POZOS_COLORADOS, HOTEL, TipoPrestador.ALOJAMIENTO,
                new VersionAlojamiento(contenido("Apartamento en Pozos Colorados",
                        "Apartamento familiar con cocina y acceso a la playa", "450000", null, ZONA_SUR, 6),
                        "apartamento", true)));
        ofertas.add(publicada(zonas, HABITACION_ECOLOGICA_MINCA, HOTEL, TipoPrestador.ALOJAMIENTO,
                new VersionAlojamiento(contenido("Habitación ecológica en Minca",
                        "Habitación con vista a la montaña y desayuno incluido", "210000", null, MINCA, 2),
                        "habitación", false)));

        // Restaurantes
        ofertas.add(publicada(zonas, COCINA_CARIBE_CENTRO_HISTORICO, RESTAURANTE, TipoPrestador.RESTAURANTE,
                new VersionRestaurante(contenido("Cocina del Caribe en el Centro Histórico",
                        "Platos tradicionales de la región en una casa colonial", "65000",
                        "Todos los días, de 12:00 a 22:00", CENTRO_HISTORICO, 12), true)));
        ofertas.add(publicada(zonas, PESCADOS_FRENTE_AL_MAR_TAGANGA, RESTAURANTE, TipoPrestador.RESTAURANTE,
                new VersionRestaurante(contenido("Pescados frente al mar en Taganga",
                        "Pesca del día frita o a la plancha, con patacón y arroz de coco", "55000",
                        "Todos los días, de 11:00 a 18:00", TAGANGA, 10), false)));
        ofertas.add(publicada(zonas, PARRILLA_RODADERO, RESTAURANTE, TipoPrestador.RESTAURANTE,
                new VersionRestaurante(contenido("Parrilla en El Rodadero",
                        "Carnes y mariscos a la parrilla", "70000",
                        "Martes a domingo, de 12:00 a 23:00", RODADERO, 16), true)));

        // Edición pendiente: el turista sigue viendo el precio aprobado mientras la nueva versión se modera (RF-66)
        caminataCaboSanJuan.editar(habilitado(AGENCIA), TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Caminata a Cabo San Juan del Guía",
                                "Sendero por la selva y las playas del parque hasta Cabo San Juan", "165000",
                                "Martes a domingo", TAYRONA, 12),
                        TipoActividad.SENDERISMO, NivelEsfuerzo.MODERADO, false, horas(7, 8), 420),
                zonas.get(TAYRONA), CARGA.plus(1, ChronoUnit.DAYS));

        // Aún sin aprobar: no aparece en la búsqueda hasta que llegue el aviso de moderación (RF-79)
        VersionActividad lanchaPlayaBlanca = actividad(contenido("Paseo en lancha a Playa Blanca",
                        "Traslado en lancha desde El Rodadero y tarde de playa", "50000",
                        "Todos los días", RODADERO, 20),
                TipoActividad.NAUTICO, NivelEsfuerzo.BAJO, false, horas(9, 10, 11), 300);
        ofertas.add(Oferta.publicar(PASEO_LANCHA_PLAYA_BLANCA, habilitado(AGENCIA),
                TipoPrestador.AGENCIA_OPERADORA, lanchaPlayaBlanca, zonas.get(RODADERO), CARGA));

        // Aprobada, pero de un prestador con el RNT inactivo: no es visible (RF-77)
        Oferta ciudadPerdida = publicada(zonas, EXPEDICION_CIUDAD_PERDIDA, AGENCIA_SIN_RNT,
                TipoPrestador.AGENCIA_OPERADORA,
                actividad(contenido("Expedición a Ciudad Perdida",
                                "Caminata de cuatro días por la Sierra Nevada hasta Teyuna", "2150000",
                                "Salidas diarias", MINCA, 12),
                        TipoActividad.SENDERISMO, NivelEsfuerzo.ALTA_EXIGENCIA, false, horas(8), 5760));
        ofertas.add(Oferta.reconstituir(ciudadPerdida.id(), ciudadPerdida.tipo(), ciudadPerdida.prestadorId(), false,
                false, ciudadPerdida.publicada().orElseThrow(), null));

        return List.copyOf(ofertas);
    }

    /** Publica la oferta con un prestador habilitado y aprueba su primera versión. */
    private static Oferta publicada(Map<String, Zona> zonas, String id, UUID prestadorId, TipoPrestador tipoPrestador,
                                    VersionOferta version, Oferta... actividades) {
        Oferta oferta = Oferta.publicar(id, habilitado(prestadorId), tipoPrestador, version,
                zonas.get(version.contenido().zonaId()), List.of(actividades), CARGA);
        oferta.aprobar(1, true, CARGA);
        return oferta;
    }

    private static PrestadorCatalogo habilitado(UUID prestadorId) {
        return new PrestadorCatalogo(prestadorId, true, 1);
    }

    private static ContenidoComun contenido(String nombre, String descripcion, String precio, String horarios,
                                            String zonaId, int capacidadMaxima) {
        return new ContenidoComun(nombre, descripcion, new BigDecimal(precio), horarios, zonaId, null, null,
                capacidadMaxima);
    }

    private static VersionActividad actividad(ContenidoComun contenido, TipoActividad tipoActividad,
                                              NivelEsfuerzo nivelEsfuerzo, boolean accesibleSillaRuedas,
                                              List<LocalTime> horariosSalida, int duracionMinutos) {
        return new VersionActividad(contenido, tipoActividad, nivelEsfuerzo, accesibleSillaRuedas, horariosSalida,
                null, duracionMinutos, null, List.of());
    }

    private static VersionActividad actividadDeGuia(ContenidoComun contenido, TipoActividad tipoActividad,
                                                    NivelEsfuerzo nivelEsfuerzo, List<LocalTime> horariosSalida,
                                                    String puntoEncuentro, int duracionMinutos,
                                                    ModalidadRecorrido modalidad, List<String> idiomas) {
        return new VersionActividad(contenido, tipoActividad, nivelEsfuerzo, false, horariosSalida, puntoEncuentro,
                duracionMinutos, modalidad, idiomas);
    }

    private static List<LocalTime> horas(int... horas) {
        return Arrays.stream(horas).mapToObj(hora -> LocalTime.of(hora, 0)).toList();
    }
}
