package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertaVisible;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.Solicitante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.ZONA_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO_OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.contenido;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ConsultarOfertasVisiblesServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final ConsultarOfertasVisiblesService servicio = escenario.visibles;

    @Test
    @DisplayName("devuelve solo las visibles, por precio, con la marca de alta afluencia de su zona (RF-29)")
    void visiblesConAltaAfluencia() {
        Oferta tayrona = escenario.ofertaPublicada("Tayrona", "90000", ZONA_ACTIVA);
        Oferta taganga = escenario.ofertaPublicada("Taganga", "40000", ZONA_TAGANGA);
        escenario.publicar(AGENCIA, actividad("Sin aprobar", "45000", ZONA_ACTIVA));

        Pagina<OfertaVisible> resultado = servicio.consultar(null, null, Paginacion.de(null, null));

        assertThat(resultado.contenido()).extracting(visible -> visible.oferta().id(),
                        OfertaVisible::zonaAltaAfluencia)
                .containsExactly(tuple(taganga.id(), false), tuple(tayrona.id(), true));
        assertThat(resultado.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("filtra por tipo y por zona, y pagina como la búsqueda pública")
    void filtrosYPaginacion() {
        Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
        Oferta kayak = escenario.ofertaPublicada("Kayak", "70000", ZONA_ACTIVA);
        Oferta paquete = escenario.ofertaPublicada(paquete(caminata.id(), kayak.id()));
        escenario.ofertaPublicada("En Taganga", "30000", ZONA_TAGANGA);

        assertThat(ids(servicio.consultar(TipoOferta.PAQUETE, null, Paginacion.de(null, null))))
                .containsExactly(paquete.id());
        assertThat(ids(servicio.consultar(TipoOferta.ACTIVIDAD, ZONA_ACTIVA, Paginacion.de(null, null))))
                .containsExactly(caminata.id(), kayak.id());
        Pagina<OfertaVisible> segunda = servicio.consultar(null, ZONA_ACTIVA, Paginacion.de(1, 2));
        assertThat(ids(segunda)).containsExactly(paquete.id());
        assertThat(segunda.totalElementos()).isEqualTo(3);
    }

    @Test
    @DisplayName("un paquete trae los tipos de actividad, el mayor esfuerzo y la accesibilidad de sus actividades")
    void datosDelPaquete() {
        Oferta buceo = actividadPublicada("Buceo", TipoActividad.BUCEO_SNORKEL, NivelEsfuerzo.ALTA_EXIGENCIA, true);
        Oferta caminata = actividadPublicada("Caminata", TipoActividad.SENDERISMO, NivelEsfuerzo.BAJO, false);
        Oferta paquete = escenario.ofertaPublicada(paquete(buceo.id(), caminata.id()));

        OfertaVisible visible = unica(TipoOferta.PAQUETE);

        assertThat(visible.oferta().id()).isEqualTo(paquete.id());
        assertThat(visible.actividades()).extracting(Oferta::id).containsExactly(buceo.id(), caminata.id());
        assertThat(visible.tiposActividad())
                .containsExactly(TipoActividad.SENDERISMO, TipoActividad.BUCEO_SNORKEL);
        assertThat(visible.nivelEsfuerzo()).contains(NivelEsfuerzo.ALTA_EXIGENCIA);
        assertThat(visible.accesibleSillaRuedas()).isFalse();
    }

    @Test
    @DisplayName("una actividad trae su tipo y su esfuerzo; un restaurante no tiene ninguno de los dos")
    void datosPorTipo() {
        actividadPublicada("Museo", TipoActividad.CULTURAL_HISTORICO, NivelEsfuerzo.BAJO, true);
        escenario.habilitar(OTRO_PRESTADOR, true, 1);
        Oferta restaurante = escenario.publicar(new Solicitante(OTRO_PRESTADOR, TipoPrestador.RESTAURANTE),
                new VersionRestaurante(contenido("Cocina del Caribe", ZONA_ACTIVA, FOTO_OTRO_PRESTADOR), true));
        escenario.moderar(restaurante.id(), 1, DecisionModeracion.APROBAR);

        OfertaVisible museo = unica(TipoOferta.ACTIVIDAD);
        assertThat(museo.tiposActividad()).containsExactly(TipoActividad.CULTURAL_HISTORICO);
        assertThat(museo.nivelEsfuerzo()).contains(NivelEsfuerzo.BAJO);
        assertThat(museo.accesibleSillaRuedas()).isTrue();

        OfertaVisible cocina = unica(TipoOferta.RESTAURANTE);
        assertThat(cocina.oferta().prestadorId()).isEqualTo(OTRO_PRESTADOR);
        assertThat(cocina.tiposActividad()).isEmpty();
        assertThat(cocina.nivelEsfuerzo()).isEmpty();
        assertThat(cocina.accesibleSillaRuedas()).isTrue();
        assertThat(cocina.actividades()).isEmpty();
    }

    private OfertaVisible unica(TipoOferta tipo) {
        List<OfertaVisible> contenido = servicio.consultar(tipo, null, Paginacion.de(null, null)).contenido();
        assertThat(contenido).hasSize(1);
        return contenido.getFirst();
    }

    private Oferta actividadPublicada(String nombre, TipoActividad tipoActividad, NivelEsfuerzo nivelEsfuerzo,
                                      boolean accesibleSillaRuedas) {
        return escenario.ofertaPublicada(new VersionActividad(contenido(nombre), tipoActividad, nivelEsfuerzo,
                accesibleSillaRuedas, List.of(LocalTime.of(7, 0)), null, null, null, List.of()));
    }

    private static List<String> ids(Pagina<OfertaVisible> pagina) {
        return pagina.contenido().stream().map(visible -> visible.oferta().id()).toList();
    }
}
