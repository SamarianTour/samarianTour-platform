package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.api.ConsultaOfertaApi;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarOfertaParaModulosService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.modulos.ConsultaOfertaApiAdapter;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.OfertaMemoriaAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.EXPEDICION_CIUDAD_PERDIDA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PASEO_LANCHA_PLAYA_BLANCA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * El módulo de opiniones pregunta al catálogo real, con los datos de demostración, solo a través de
 * {@link ConsultaOfertaApi} (DC-40).
 */
class OfertaCatalogoAdapterTest {

    private final OfertaCatalogoAdapter adaptador = new OfertaCatalogoAdapter(
            new ConsultaOfertaApiAdapter(new ConsultarOfertaParaModulosService(new OfertaMemoriaAdapter())));

    @Test
    @DisplayName("una oferta visible existe y es visible; una de un prestador sin RNT existe pero no es visible")
    void existenciaYVisibilidad() {
        assertThat(adaptador.existe(CAMINATA_CABO_SAN_JUAN)).isTrue();
        assertThat(adaptador.esVisible(CAMINATA_CABO_SAN_JUAN)).isTrue();

        assertThat(adaptador.existe(EXPEDICION_CIUDAD_PERDIDA)).isTrue();
        assertThat(adaptador.esVisible(EXPEDICION_CIUDAD_PERDIDA)).isFalse();

        assertThat(adaptador.existe("b000000000000000000000ff")).isFalse();
        assertThat(adaptador.esVisible("b000000000000000000000ff")).isFalse();
    }

    @Test
    @DisplayName("da el nombre de las ofertas que existen, también de una que solo está en moderación")
    void nombres() {
        assertThat(adaptador.nombres(List.of(CAMINATA_CABO_SAN_JUAN, PASEO_LANCHA_PLAYA_BLANCA, "no-existe")))
                .containsOnlyKeys(CAMINATA_CABO_SAN_JUAN, PASEO_LANCHA_PLAYA_BLANCA)
                .containsEntry(CAMINATA_CABO_SAN_JUAN, "Caminata a Cabo San Juan del Guía");
    }
}
