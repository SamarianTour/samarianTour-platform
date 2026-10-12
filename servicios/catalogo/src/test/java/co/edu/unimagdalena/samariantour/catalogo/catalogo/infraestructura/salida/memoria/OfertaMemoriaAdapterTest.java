package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El adaptador en memoria con los datos de demostración de Santa Marta. */
class OfertaMemoriaAdapterTest {

    private final OfertaMemoriaAdapter ofertas = new OfertaMemoriaAdapter();

    @Test
    @DisplayName("entrega copias: un cambio que no se guarda no queda registrado")
    void guardaYEntregaCopias() {
        Oferta leida = ofertas.buscarPorId("recorrido-centro-historico").orElseThrow();

        leida.retirar(DatosSantaMarta.AGENCIA);
        assertThat(ofertas.buscarPorId("recorrido-centro-historico").orElseThrow().retirada()).isFalse();

        ofertas.guardar(leida);
        assertThat(ofertas.buscarPorId("recorrido-centro-historico").orElseThrow().retirada()).isTrue();
    }

    @Test
    @DisplayName("la búsqueda solo trae ofertas visibles, ordenadas por precio")
    void soloVisiblesOrdenadasPorPrecio() {
        Pagina<Oferta> todas = buscar(null, null, null, null, false, 100);

        assertThat(todas.contenido()).extracting(Oferta::id)
                .doesNotContain("paseo-lancha-playa-blanca", "expedicion-ciudad-perdida");
        assertThat(todas.totalElementos()).isEqualTo(DatosSantaMarta.ofertas().size() - 2);
        assertThat(todas.contenido())
                .extracting(oferta -> oferta.publicada().orElseThrow().contenido().precioReferencia())
                .isSorted();
    }

    @Test
    @DisplayName("filtra por tipo, zona y precio")
    void filtrosBasicos() {
        assertThat(ids(buscar(TipoOferta.RESTAURANTE, DatosSantaMarta.TAGANGA, null, null, false, 100)))
                .containsExactly("pescados-frente-al-mar-taganga");

        CriteriosBusqueda hastaCienMil = new CriteriosBusqueda(TipoOferta.ACTIVIDAD, DatosSantaMarta.MINCA, null,
                new BigDecimal("100000"), null, null, false, Paginacion.de(null, null));
        assertThat(ids(ofertas.buscarVisibles(hastaCienMil)))
                .containsExactly("ruta-cafe-cacao-minca", "caminata-pozo-azul-marinka");
    }

    @Test
    @DisplayName("el filtro por tipo de actividad incluye los paquetes que tienen una de ese tipo (DC-24)")
    void porTipoDeActividad() {
        assertThat(ids(buscar(null, null, TipoActividad.BUCEO_SNORKEL, null, false, 100)))
                .containsExactlyInAnyOrder("snorkel-playa-cristal", "bautizo-buceo-taganga",
                        "paquete-tayrona-mar-y-selva", "paquete-taganga-bajo-el-agua");
    }

    @Test
    @DisplayName("el filtro por nivel de esfuerzo es hasta el indicado y deja fuera alojamientos y restaurantes (DC-25)")
    void hastaElNivelDeEsfuerzo() {
        Pagina<Oferta> bajas = buscar(null, null, null, NivelEsfuerzo.BAJO, false, 100);

        assertThat(bajas.contenido()).extracting(Oferta::tipo)
                .containsOnly(TipoOferta.ACTIVIDAD, TipoOferta.PAQUETE);
        assertThat(ids(bajas)).contains("snorkel-playa-cristal", "paquete-minca-aves-y-cafe")
                .doesNotContain("caminata-cabo-san-juan", "paquete-tayrona-mar-y-selva",
                        "descenso-bicicleta-sierra");
        assertThat(ids(buscar(null, null, null, NivelEsfuerzo.ALTA_EXIGENCIA, false, 100)))
                .contains("descenso-bicicleta-sierra", "paquete-tayrona-mar-y-selva");
    }

    @Test
    @DisplayName("filtra las ofertas accesibles en silla de ruedas (RF-17)")
    void soloAccesibles() {
        assertThat(ids(buscar(null, null, null, null, true, 100)))
                .containsExactlyInAnyOrder("recorrido-centro-historico", "habitacion-doble-rodadero",
                        "apartamento-pozos-colorados", "cocina-caribe-centro-historico", "parrilla-rodadero");
    }

    @Test
    @DisplayName("pagina los resultados sin repetir ni saltar ofertas")
    void paginacion() {
        Pagina<Oferta> primera = ofertas.buscarVisibles(criterios(null, null, null, null, false, new Paginacion(0, 5)));
        Pagina<Oferta> segunda = ofertas.buscarVisibles(criterios(null, null, null, null, false, new Paginacion(1, 5)));

        assertThat(primera.contenido()).hasSize(5);
        assertThat(ids(segunda)).hasSize(5).doesNotContainAnyElementsOf(ids(primera));
        assertThat(primera.totalElementos()).isEqualTo(segunda.totalElementos());
    }

    @Test
    @DisplayName("al cambiar el RNT de un prestador, sus ofertas y paquetes dejan de ser visibles y vuelven (RF-77)")
    void actualizarRntPrestador() {
        ofertas.actualizarRntPrestador(DatosSantaMarta.AGENCIA, false);
        assertThat(buscar(null, null, null, null, false, 100).contenido())
                .noneMatch(oferta -> oferta.esDe(DatosSantaMarta.AGENCIA));

        ofertas.actualizarRntPrestador(DatosSantaMarta.AGENCIA, true);
        assertThat(ids(buscar(null, null, null, null, false, 100)))
                .contains("caminata-cabo-san-juan", "paquete-tayrona-mar-y-selva");
    }

    @Test
    @DisplayName("lista las ofertas de un prestador en cualquier estado, ordenadas por id")
    void porPrestador() {
        Pagina<Oferta> delGuia = ofertas.buscarPorPrestador(DatosSantaMarta.GUIA, Paginacion.de(null, null));

        assertThat(ids(delGuia)).containsExactly("avistamiento-aves-minca", "caminata-pozo-azul-marinka",
                "descenso-bicicleta-sierra", "paquete-minca-aves-y-cafe", "ruta-cafe-cacao-minca");
        assertThat(ids(ofertas.buscarPorPrestador(DatosSantaMarta.AGENCIA, Paginacion.de(null, null))))
                .contains("paseo-lancha-playa-blanca");
    }

    private Pagina<Oferta> buscar(TipoOferta tipo, String zonaId, TipoActividad tipoActividad,
                                  NivelEsfuerzo nivelEsfuerzoMaximo, boolean soloAccesibles, int tamano) {
        return ofertas.buscarVisibles(criterios(tipo, zonaId, tipoActividad, nivelEsfuerzoMaximo, soloAccesibles,
                new Paginacion(0, tamano)));
    }

    private static CriteriosBusqueda criterios(TipoOferta tipo, String zonaId, TipoActividad tipoActividad,
                                               NivelEsfuerzo nivelEsfuerzoMaximo, boolean soloAccesibles,
                                               Paginacion paginacion) {
        return new CriteriosBusqueda(tipo, zonaId, null, null, tipoActividad, nivelEsfuerzoMaximo, soloAccesibles,
                paginacion);
    }

    private static List<String> ids(Pagina<Oferta> pagina) {
        return pagina.contenido().stream().map(Oferta::id).toList();
    }
}
