package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.ZONA_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
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

        Pagina<Oferta> resultado = servicio.buscar(new CriteriosBusqueda(TipoOferta.ACTIVIDAD, ZONA_ACTIVA,
                new BigDecimal("30000"), new BigDecimal("100000"), Paginacion.de(null, null)));

        assertThat(resultado.contenido()).extracting(Oferta::id).containsExactly(barata.id(), cara.id());
        assertThat(resultado.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("pagina los resultados")
    void paginacion() {
        for (int i = 1; i <= 5; i++) {
            escenario.ofertaPublicada("Oferta " + i, String.valueOf(i * 10000), ZONA_ACTIVA);
        }

        Pagina<Oferta> segunda = servicio.buscar(new CriteriosBusqueda(null, null, null, null, Paginacion.de(1, 2)));

        assertThat(segunda.contenido()).extracting(EscenarioAplicacion::nombrePublicado)
                .containsExactly("Oferta 3", "Oferta 4");
        assertThat(segunda.totalElementos()).isEqualTo(5);
        assertThat(segunda.totalPaginas()).isEqualTo(3);
    }
}
