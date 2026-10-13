package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DatosSantaMartaTest {

    private final List<Oferta> ofertas = DatosSantaMarta.ofertas();
    private final Map<String, Oferta> porId = ofertas.stream().collect(Collectors.toMap(Oferta::id, Function.identity()));

    @Test
    @DisplayName("las zonas son las siete de la precarga, con las marcas que DC-18 sí define")
    void zonas() {
        Map<String, Zona> zonas = DatosSantaMarta.zonas().stream()
                .collect(Collectors.toMap(Zona::id, Function.identity()));

        assertThat(zonas).hasSize(7);
        assertThat(zonas.values()).allMatch(Zona::activa);
        assertThat(zonas.get(DatosSantaMarta.TAYRONA)).matches(Zona::areaProtegida).matches(Zona::altaAfluencia);
        assertThat(zonas.get(DatosSantaMarta.TAGANGA)).matches(zona -> !zona.areaProtegida())
                .matches(Zona::altaAfluencia);
        assertThat(zonas.get(DatosSantaMarta.MINCA)).matches(Zona::areaProtegida)
                .matches(zona -> !zona.altaAfluencia());
        assertThat(zonas.get(DatosSantaMarta.CENTRO_HISTORICO)).matches(zona -> !zona.areaProtegida())
                .matches(zona -> !zona.altaAfluencia());
    }

    @Test
    @DisplayName("hay unas veinte ofertas de los cuatro tipos, con al menos dos paquetes")
    void ofertasDeLosCuatroTipos() {
        assertThat(ofertas).hasSizeGreaterThanOrEqualTo(20);
        assertThat(porId).hasSameSizeAs(ofertas);
        assertThat(ofertas).extracting(Oferta::tipo).contains(TipoOferta.values());
        assertThat(ofertas).filteredOn(oferta -> oferta.tipo() == TipoOferta.PAQUETE)
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("toda oferta es de un prestador de la demostración y está en una zona de la precarga")
    void referenciasValidas() {
        List<String> zonas = DatosSantaMarta.zonas().stream().map(Zona::id).toList();
        List<java.util.UUID> prestadores = DatosSantaMarta.prestadores().stream()
                .map(PrestadorCatalogo::prestadorId).toList();

        assertThat(ofertas).allSatisfy(oferta -> {
            assertThat(prestadores).contains(oferta.prestadorId());
            assertThat(zonas).contains(oferta.publicada().or(oferta::pendiente).orElseThrow().contenido().zonaId());
        });
    }

    @Test
    @DisplayName("todos los paquetes son visibles: sus actividades existen y están publicadas")
    void paquetesVisibles() {
        assertThat(ofertas).filteredOn(oferta -> oferta.tipo() == TipoOferta.PAQUETE).allSatisfy(paquete -> {
            List<Oferta> actividades = ReglasPaquete.actividadIdsDe(paquete).stream().map(porId::get).toList();
            assertThat(ReglasPaquete.esVisible(paquete, actividades)).isTrue();
        });
    }

    @Test
    @DisplayName("un prestador tiene el RNT inactivo y su oferta aprobada no es visible (RF-77)")
    void prestadorConRntInactivo() {
        assertThat(DatosSantaMarta.prestadores())
                .filteredOn(prestador -> !prestador.rntActivo())
                .extracting(PrestadorCatalogo::prestadorId).containsExactly(DatosSantaMarta.AGENCIA_SIN_RNT);
        assertThat(ofertas).filteredOn(oferta -> oferta.esDe(DatosSantaMarta.AGENCIA_SIN_RNT))
                .isNotEmpty()
                .allSatisfy(oferta -> {
                    assertThat(oferta.publicada()).isPresent();
                    assertThat(oferta.estaVisible()).isFalse();
                });
    }

    @Test
    @DisplayName("una oferta tiene una edición pendiente y sigue mostrando la versión aprobada (RF-66)")
    void ofertaConEdicionPendiente() {
        Oferta caminata = porId.get("caminata-cabo-san-juan");

        assertThat(caminata.estaVisible()).isTrue();
        assertThat(caminata.publicada().orElseThrow().contenido().precioReferencia()).isEqualByComparingTo("150000");
        assertThat(caminata.pendiente()).hasValueSatisfying(pendiente -> {
            assertThat(pendiente.numero()).isEqualTo(2);
            assertThat(pendiente.estadoModeracion()).isEqualTo(EstadoModeracion.EN_MODERACION);
            assertThat(pendiente.contenido().precioReferencia()).isEqualByComparingTo("165000");
        });
    }

    @Test
    @DisplayName("una oferta sigue en moderación y aún no es visible (RF-79)")
    void ofertaSinAprobar() {
        Oferta lancha = porId.get("paseo-lancha-playa-blanca");

        assertThat(lancha.publicada()).isEmpty();
        assertThat(lancha.estaVisible()).isFalse();
    }
}
