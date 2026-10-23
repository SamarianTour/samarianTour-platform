package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.ZONA_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.contenido;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static org.assertj.core.api.Assertions.assertThat;

class BuscarOfertasServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final BuscarOfertasService servicio = escenario.busqueda;

    @Test
    @DisplayName("devuelve solo ofertas visibles que cumplen los filtros, ordenadas por precio")
    void conFiltros() {
        Oferta barata = escenario.ofertaPublicada("Barata", "40000", ZONA_ACTIVA);
        Oferta cara = escenario.ofertaPublicada("Cara", "90000", ZONA_ACTIVA);
        escenario.ofertaPublicada("En Taganga", "50000", ZONA_TAGANGA);
        escenario.publicar(AGENCIA, actividad("Sin aprobar", "45000", ZONA_ACTIVA));

        Pagina<Oferta> resultado = servicio.buscar(new CriteriosBusqueda(null, TipoOferta.ACTIVIDAD, ZONA_ACTIVA,
                new BigDecimal("30000"), new BigDecimal("100000"), null, null, false, Paginacion.de(null, null)));

        assertThat(resultado.contenido()).extracting(Oferta::id).containsExactly(barata.id(), cara.id());
        assertThat(resultado.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("pagina los resultados")
    void paginacion() {
        for (int i = 1; i <= 5; i++) {
            escenario.ofertaPublicada("Oferta " + i, String.valueOf(i * 10000), ZONA_ACTIVA);
        }

        Pagina<Oferta> segunda = servicio.buscar(new CriteriosBusqueda(null, null, null, null, null, null, null, false,
                Paginacion.de(1, 2)));

        assertThat(segunda.contenido()).extracting(EscenarioAplicacion::nombrePublicado)
                .containsExactly("Oferta 3", "Oferta 4");
        assertThat(segunda.totalElementos()).isEqualTo(5);
        assertThat(segunda.totalPaginas()).isEqualTo(3);
    }

    @Nested
    @DisplayName("Tipo de actividad, esfuerzo y accesibilidad (RF-15 a RF-17)")
    class FiltrosDeActividad {

        private final Oferta caminata = actividadPublicada("Caminata", TipoActividad.SENDERISMO,
                NivelEsfuerzo.BAJO, true);
        private final Oferta museo = actividadPublicada("Museo", TipoActividad.CULTURAL_HISTORICO,
                NivelEsfuerzo.BAJO, true);
        private final Oferta buceo = actividadPublicada("Buceo", TipoActividad.BUCEO_SNORKEL,
                NivelEsfuerzo.ALTA_EXIGENCIA, false);

        @Test
        @DisplayName("filtra por tipo de actividad e incluye los paquetes con al menos una de ese tipo (DC-24)")
        void porTipoDeActividad() {
            Oferta paqueteConBuceo = paquetePublicado(caminata, buceo);
            paquetePublicado(caminata, museo);

            assertThat(buscar(TipoActividad.BUCEO_SNORKEL, null, false))
                    .containsExactlyInAnyOrder(buceo.id(), paqueteConBuceo.id());
        }

        @Test
        @DisplayName("filtra hasta el nivel de esfuerzo indicado; el del paquete es el mayor de sus actividades (DC-25)")
        void hastaElNivelDeEsfuerzo() {
            Oferta kayak = actividadPublicada("Kayak", TipoActividad.DEPORTES_ACUATICOS, NivelEsfuerzo.MODERADO,
                    false);
            Oferta paqueteExigente = paquetePublicado(caminata, buceo);
            Oferta paqueteModerado = paquetePublicado(caminata, kayak);
            Oferta paqueteSuave = paquetePublicado(caminata, museo);

            assertThat(buscar(null, NivelEsfuerzo.BAJO, false))
                    .containsExactlyInAnyOrder(caminata.id(), museo.id(), paqueteSuave.id());
            assertThat(buscar(null, NivelEsfuerzo.MODERADO, false))
                    .containsExactlyInAnyOrder(caminata.id(), museo.id(), paqueteSuave.id(), kayak.id(),
                            paqueteModerado.id());
            assertThat(buscar(null, NivelEsfuerzo.ALTA_EXIGENCIA, false))
                    .containsExactlyInAnyOrder(caminata.id(), museo.id(), paqueteSuave.id(), kayak.id(),
                            paqueteModerado.id(), buceo.id(), paqueteExigente.id());
        }

        @Test
        @DisplayName("filtra las accesibles en silla de ruedas; el paquete lo es si todas sus actividades lo son")
        void soloAccesibles() {
            paquetePublicado(caminata, buceo);
            Oferta paqueteAccesible = paquetePublicado(caminata, museo);

            assertThat(buscar(null, null, true))
                    .containsExactlyInAnyOrder(caminata.id(), museo.id(), paqueteAccesible.id());
        }

        @Test
        @DisplayName("un paquete deja de aparecer cuando se retira una de sus actividades (RF-22)")
        void paqueteConActividadRetirada() {
            Oferta paquete = paquetePublicado(caminata, museo);
            assertThat(buscar(null, null, false)).contains(paquete.id());

            escenario.gestion.retirar(new RetirarOfertaCommand(museo.id(), PRESTADOR));

            assertThat(buscar(null, null, false)).containsExactlyInAnyOrder(caminata.id(), buceo.id());
        }

        private List<String> buscar(TipoActividad tipoActividad, NivelEsfuerzo nivelEsfuerzoMaximo,
                                    boolean soloAccesibles) {
            return servicio.buscar(new CriteriosBusqueda(null, null, null, null, null, tipoActividad,
                            nivelEsfuerzoMaximo, soloAccesibles, Paginacion.de(null, null)))
                    .contenido().stream().map(Oferta::id).toList();
        }

        private Oferta actividadPublicada(String nombre, TipoActividad tipoActividad, NivelEsfuerzo nivelEsfuerzo,
                                          boolean accesibleSillaRuedas) {
            return escenario.ofertaPublicada(new VersionActividad(contenido(nombre), tipoActividad, nivelEsfuerzo,
                    accesibleSillaRuedas, List.of(LocalTime.of(7, 0)), null, null, null, List.of()));
        }

        private Oferta paquetePublicado(Oferta... actividades) {
            return escenario.ofertaPublicada(
                    paquete(Arrays.stream(actividades).map(Oferta::id).toArray(String[]::new)));
        }
    }
}
