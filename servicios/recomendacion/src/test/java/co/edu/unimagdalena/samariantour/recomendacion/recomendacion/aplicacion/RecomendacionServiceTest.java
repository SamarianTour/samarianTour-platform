package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.MotorNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.PoliticaRotacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ReglaCriteriosExplicitos;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.RecommendationPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.reglas.RecomendadorPorReglasAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.preferencias;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion.POCAS_COINCIDENCIAS;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion.IA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion.REGLAS;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.BUCEO_SNORKEL;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.SENDERISMO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecomendacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    private final List<RegistroExposicion> exposiciones = new ArrayList<>();
    private final RecomendadorPorReglasAdapter reglas = new RecomendadorPorReglasAdapter();
    private final Preferencias senderismo = preferencias().actividades(SENDERISMO).crear();

    /** Seis caminatas, dos de ellas accesibles, cada una de un prestador distinto. */
    private final List<OfertaCandidata> catalogo = IntStream.rangeClosed(1, 6)
            .mapToObj(i -> oferta("o" + i).accesible(i <= 2)
                    .prestador(UUID.fromString("00000000-0000-0000-0000-00000000000" + i)).crear())
            .toList();

    @Test
    @DisplayName("sin IA, responde el motor por reglas con 5 recomendaciones y registra la exposición")
    void porReglas() {
        ResultadoRecomendacion resultado = servicio(() -> catalogo, reglas).recomendar(senderismo);

        assertThat(resultado.motor()).isEqualTo(REGLAS);
        assertThat(resultado.aviso()).isNull();
        assertThat(resultado.recomendaciones()).hasSize(5);
        assertThat(exposiciones).singleElement().satisfies(registro -> {
            assertThat(registro.fecha()).isEqualTo(AHORA);
            assertThat(registro.motor()).isEqualTo(REGLAS);
            assertThat(registro.ofertas()).hasSize(5).allSatisfy(o -> assertThat(o.prestadorId()).isNotNull());
        });
    }

    @Test
    @DisplayName("si la IA responde, el resultado dice IA")
    void conIA() {
        ResultadoRecomendacion resultado = servicio(() -> catalogo, motor(IA, false)).recomendar(senderismo);

        assertThat(resultado.motor()).isEqualTo(IA);
        assertThat(exposiciones.getFirst().motor()).isEqualTo(IA);
    }

    @Test
    @DisplayName("si la IA falla, responde el motor por reglas sin error para el turista (QAS-03)")
    void iaFalla() {
        ResultadoRecomendacion resultado = servicio(() -> catalogo, motor(IA, true)).recomendar(senderismo);

        assertThat(resultado.motor()).isEqualTo(REGLAS);
        assertThat(resultado.recomendaciones()).hasSize(5);
        assertThat(exposiciones.getFirst().motor()).isEqualTo(REGLAS);
    }

    @Test
    @DisplayName("si el catálogo falla sin caché, RECOMENDACION_NO_DISPONIBLE (503) y no se registra exposición")
    void catalogoFalla() {
        CatalogoConsultaPort caido = () -> {
            throw new RecomendacionNoDisponibleException("El catálogo no responde");
        };

        assertThatThrownBy(() -> servicio(caido, reglas).recomendar(senderismo))
                .isInstanceOf(RecomendacionNoDisponibleException.class);
        assertThat(exposiciones).isEmpty();
    }

    @Test
    @DisplayName("si solo 2 pasan los filtros duros, devuelve esas 2 con POCAS_COINCIDENCIAS, sin relajar el filtro")
    void pocasCoincidencias() {
        ResultadoRecomendacion resultado = servicio(() -> catalogo, reglas)
                .recomendar(preferencias().accesible().crear());

        assertThat(resultado.aviso()).isEqualTo(POCAS_COINCIDENCIAS);
        assertThat(resultado.recomendaciones()).hasSize(2)
                .allSatisfy(r -> assertThat(r.oferta().accesibleSillaRuedas()).isTrue());
    }

    @Test
    @DisplayName("si ninguna pasa, 0 recomendaciones con el aviso y sin llamar a la IA")
    void ninguna() {
        ResultadoRecomendacion resultado = servicio(() -> catalogo, motor(IA, true))
                .recomendar(preferencias().precioMaximo(1_000).crear());

        assertThat(resultado.recomendaciones()).isEmpty();
        assertThat(resultado.aviso()).isEqualTo(POCAS_COINCIDENCIAS);
        assertThat(resultado.motor()).isEqualTo(REGLAS);
    }

    @Test
    @DisplayName("con solo texto, primero las ofertas que coinciden con sus palabras, con cualquier semilla")
    void soloTexto() {
        List<OfertaCandidata> conSnorkel = new ArrayList<>(catalogo);
        conSnorkel.add(oferta("snorkel").nombre("Snorkel en Playa Cristal").actividades(BUCEO_SNORKEL).crear());

        for (long semilla = 0; semilla < 10; semilla++) {
            ResultadoRecomendacion resultado = servicio(() -> conSnorkel, reglas, semilla)
                    .recomendar(preferencias().texto("quiero hacer snorkeling").crear());
            assertThat(resultado.recomendaciones().getFirst().oferta().id()).as("semilla %d", semilla)
                    .isEqualTo("snorkel");
        }
    }

    @Test
    @DisplayName("el motor recibe las candidatas ya rotadas: con otra semilla, otro orden entre empatadas")
    void rotacion() {
        List<String> conSemilla1 = ids(servicio(() -> catalogo, reglas, 1).recomendar(senderismo));
        List<String> conSemilla1OtraVez = ids(servicio(() -> catalogo, reglas, 1).recomendar(senderismo));
        List<String> conSemilla2 = ids(servicio(() -> catalogo, reglas, 2).recomendar(senderismo));

        assertThat(conSemilla1).isEqualTo(conSemilla1OtraVez);
        assertThat(conSemilla1).isNotEqualTo(conSemilla2);
    }

    private RecomendacionService servicio(CatalogoConsultaPort catalogo, RecommendationPort motor) {
        return servicio(catalogo, motor, 7);
    }

    private RecomendacionService servicio(CatalogoConsultaPort catalogo, RecommendationPort motor, long semilla) {
        return new RecomendacionService(catalogo, motor, reglas, exposiciones::add, new ReglaCriteriosExplicitos(),
                new PoliticaRotacion(new Random(semilla)), Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    /** Un motor de prueba que falla o devuelve las 3 primeras candidatas. */
    private static RecommendationPort motor(MotorRecomendacion tipo, boolean falla) {
        return new RecommendationPort() {
            @Override
            public MotorRecomendacion motor() {
                return tipo;
            }

            @Override
            public List<Recomendacion> recomendar(Preferencias preferencias, List<OfertaEvaluada> candidatas) {
                if (falla) {
                    throw new MotorNoDisponibleException("TIMEOUT");
                }
                return IntStream.range(0, 3).mapToObj(i -> new Recomendacion(i + 1, candidatas.get(i).oferta(),
                        new Explicacion(List.copyOf(candidatas.get(i).criteriosCumplidos()), "Explicación de la IA.")))
                        .toList();
            }
        };
    }

    private static List<String> ids(ResultadoRecomendacion resultado) {
        return resultado.recomendaciones().stream().map(r -> r.oferta().id()).toList();
    }
}
