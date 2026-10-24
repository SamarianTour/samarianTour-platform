package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.ResultadoEnvio;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.config.BandejaConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Cómo se traduce cada respuesta del catálogo (DN-06), contra un catálogo simulado. */
class CatalogoAvisosRestAdapterTest {

    private static final CatalogoSimulado CATALOGO = new CatalogoSimulado();
    private static final String AVISO = "{\"idAviso\":\"a-1\",\"tipo\":\"SUPRESION_TURISTA\"}";

    private final CatalogoAvisosRestAdapter adaptador =
            new CatalogoAvisosRestAdapter(BandejaConfig.clienteCatalogo(CATALOGO.url()));

    @BeforeEach
    void reiniciar() {
        CATALOGO.reiniciar();
    }

    @AfterAll
    static void apagar() {
        CATALOGO.close();
    }

    @Test
    @DisplayName("2xx es recibido; envía el JSON tal cual a la entrada de su destino")
    void recibido() {
        assertThat(adaptador.enviar(Destino.OPINIONES, AVISO)).isInstanceOf(ResultadoEnvio.Recibido.class);
        assertThat(adaptador.enviar(Destino.OFERTAS, AVISO)).isInstanceOf(ResultadoEnvio.Recibido.class);

        assertThat(CATALOGO.recibidas()).containsExactly(
                new CatalogoSimulado.Peticion("/internal/opiniones/avisos", AVISO),
                new CatalogoSimulado.Peticion("/internal/avisos", AVISO));
    }

    @Test
    @DisplayName("un 4xx es rechazado, salvo el 409 de concurrencia, que se reintenta (ADR-05)")
    void rechazado() {
        CATALOGO.responder(400);
        assertThat(adaptador.enviar(Destino.OFERTAS, AVISO)).isInstanceOf(ResultadoEnvio.Rechazado.class);

        CATALOGO.responder(409);
        assertThat(adaptador.enviar(Destino.OFERTAS, AVISO)).isInstanceOf(ResultadoEnvio.Fallido.class);
    }

    @Test
    @DisplayName("un 5xx, una respuesta que tarda más de 3 s o un catálogo apagado son fallidos")
    void fallido() {
        CATALOGO.responder(503);
        assertThat(adaptador.enviar(Destino.OFERTAS, AVISO)).isInstanceOf(ResultadoEnvio.Fallido.class);

        CATALOGO.responder(200);
        CATALOGO.demorar(Duration.ofSeconds(4));
        assertThat(adaptador.enviar(Destino.OFERTAS, AVISO)).isInstanceOf(ResultadoEnvio.Fallido.class);

        var apagado = new CatalogoAvisosRestAdapter(BandejaConfig.clienteCatalogo("http://localhost:1/api"));
        assertThat(apagado.enviar(Destino.OFERTAS, AVISO)).isInstanceOf(ResultadoEnvio.Fallido.class);
    }
}
