package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OfertaEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OpinionEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.compartido.config.ClienteCatalogoConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las consultas de moderación al catálogo, contra un catálogo simulado (DN-08). */
class ModeracionCatalogoRestAdapterTest {

    private static final CatalogoSimulado CATALOGO = new CatalogoSimulado();
    private static final UUID PRESTADOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TURISTA = UUID.fromString("80000000-0000-0000-0000-000000000008");

    private final ModeracionCatalogoRestAdapter adaptador =
            new ModeracionCatalogoRestAdapter(ClienteCatalogoConfig.crear(CATALOGO.url()));

    @BeforeEach
    void reiniciar() {
        CATALOGO.reiniciar();
    }

    @AfterAll
    static void apagar() {
        CATALOGO.close();
    }

    @Test
    @DisplayName("lee la oferta y la opinión en moderación con los campos del contrato del catálogo")
    void lee() {
        CATALOGO.responder("/internal/moderacion/ofertas/b17", 200, """
                {"id": "b17", "tipo": "ACTIVIDAD", "nombre": "Paseo en lancha", "prestadorId": "%s",
                 "numeroVersion": 1, "enviadaEn": "2026-10-01T13:00:00Z"}""".formatted(PRESTADOR));
        CATALOGO.responder("/internal/moderacion/opiniones/d01", 200, """
                {"id": "d01", "ofertaId": "b06", "turistaId": "%s", "estado": "EN_MODERACION",
                 "fecha": "2026-10-01T13:00:00Z"}""".formatted(TURISTA));

        assertThat(adaptador.ofertaEnModeracion("b17"))
                .contains(new OfertaEnModeracion("b17", "Paseo en lancha", PRESTADOR, 1));
        assertThat(adaptador.opinionEnModeracion("d01")).contains(new OpinionEnModeracion("d01", "b06", TURISTA));
    }

    @Test
    @DisplayName("un 404 es que no está en moderación; un 5xx o un catálogo apagado, que no está disponible")
    void ausenteOCaido() {
        CATALOGO.responder(404);
        assertThat(adaptador.ofertaEnModeracion("b99")).isEmpty();

        CATALOGO.responder(503);
        assertThatThrownBy(() -> adaptador.opinionEnModeracion("d01"))
                .isInstanceOf(CatalogoNoDisponibleException.class);

        var apagado = new ModeracionCatalogoRestAdapter(ClienteCatalogoConfig.crear("http://localhost:1/api"));
        assertThatThrownBy(() -> apagado.ofertaEnModeracion("b17"))
                .isInstanceOf(CatalogoNoDisponibleException.class);
    }
}
