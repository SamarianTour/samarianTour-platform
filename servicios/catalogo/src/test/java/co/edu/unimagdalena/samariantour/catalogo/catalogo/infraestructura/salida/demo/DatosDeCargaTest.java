package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** El catálogo sintético de la prueba de carga (QAS-07, DC-48). */
class DatosDeCargaTest {

    private static final int CANTIDAD = 5000;
    private static final List<Oferta> OFERTAS = DatosDeCarga.ofertas(CANTIDAD);

    @Test
    @DisplayName("genera exactamente las ofertas pedidas, con identificadores fijos que no se cruzan con los de la demo")
    void cantidadEIdentificadores() {
        assertThat(OFERTAS).hasSize(CANTIDAD);
        assertThat(OFERTAS).extracting(Oferta::id).doesNotHaveDuplicates()
                .first().isEqualTo(DatosDeCarga.idDeOferta(1));
        assertThat(OFERTAS.getLast().id()).isEqualTo(DatosDeCarga.idDeOferta(CANTIDAD));
        Set<String> demo = DatosSantaMarta.ofertas().stream().map(Oferta::id).collect(Collectors.toSet());
        assertThat(OFERTAS).extracting(Oferta::id).doesNotContainAnyElementsOf(demo);
        assertThat(DatosDeCarga.ofertas(7)).hasSize(7);
    }

    @Test
    @DisplayName("todas las réplicas generan los mismos datos")
    void determinista() {
        assertThat(DatosDeCarga.ofertas(CANTIDAD)).map(DatosDeCargaTest::huella)
                .containsExactlyElementsOf(OFERTAS.stream().map(DatosDeCargaTest::huella).toList());
    }

    /** Lo que distingue a una oferta generada: su estado y el contenido de sus versiones. */
    private static String huella(Oferta oferta) {
        return String.join("|", oferta.id(), oferta.prestadorId().toString(), String.valueOf(oferta.retirada()),
                String.valueOf(oferta.rntPrestadorActivo()), oferta.publicada().map(v -> v.contenido().toString())
                        .orElse("-"), oferta.pendiente().map(v -> v.contenido().toString()).orElse("-"));
    }

    @Test
    @DisplayName("cada oferta es de un prestador sintético; uno de cada 20 perdió el RNT")
    void prestadores() {
        List<PrestadorCatalogo> prestadores = DatosDeCarga.prestadores(CANTIDAD);
        assertThat(prestadores).hasSize(200);
        assertThat(prestadores).filteredOn(prestador -> !prestador.rntActivo()).hasSize(10);
        Set<UUID> ids = prestadores.stream().map(PrestadorCatalogo::prestadorId).collect(Collectors.toSet());
        assertThat(OFERTAS).allSatisfy(oferta -> assertThat(ids).contains(oferta.prestadorId()));
        assertThat(OFERTAS).filteredOn(oferta -> !oferta.rntPrestadorActivo()).hasSize(10 * 25);
    }

    @Test
    @DisplayName("mezcla los cuatro tipos en las siete zonas y estados como los de un catálogo en uso")
    void mezcla() {
        assertThat(OFERTAS).extracting(Oferta::tipo).containsAll(Set.of(TipoOferta.values()));
        Set<String> zonas = DatosSantaMarta.zonas().stream().map(Zona::id).collect(Collectors.toSet());
        assertThat(OFERTAS.stream().map(oferta -> oferta.pendiente().or(oferta::publicada).orElseThrow()
                .contenido().zonaId()).collect(Collectors.toSet())).isEqualTo(zonas);

        assertThat(proporcion(Oferta::estaVisible)).isBetween(0.70, 0.85);
        assertThat(proporcion(oferta -> oferta.publicada().isEmpty())).isBetween(0.04, 0.10);
        assertThat(proporcion(oferta -> oferta.publicada().isPresent() && oferta.pendiente().isPresent()))
                .isBetween(0.02, 0.10);
        assertThat(proporcion(Oferta::retirada)).isBetween(0.03, 0.08);
    }

    private static double proporcion(Predicate<Oferta> condicion) {
        return (double) OFERTAS.stream().filter(condicion).count() / OFERTAS.size();
    }
}
