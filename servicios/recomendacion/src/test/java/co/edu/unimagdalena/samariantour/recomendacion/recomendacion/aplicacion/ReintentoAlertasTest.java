package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.CatalogoDePrueba;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.AlertasNoDisponiblesException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoFuenteAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ActualizarAlertasUseCase.Reintento;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.WeatherAlertPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.AlertaCacheCaffeineAdapter;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.IdeamAlertasHidrologicasAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.MINCA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El reintento de las alertas mientras la caché no tiene ninguna lectura buena (DR-16).
 */
class ReintentoAlertasTest {

    private static final Instant ARRANQUE = Instant.parse("2026-10-24T15:00:00Z");
    private static final Instant REINTENTO = ARRANQUE.plusSeconds(120);

    private final AlertaCacheCaffeineAdapter cache = new AlertaCacheCaffeineAdapter();
    private final CatalogoConsultaPort catalogo = CatalogoDePrueba.de(List.of(),
            List.of(new ZonaCatalogo(MINCA, "Minca y Sierra Nevada", 11.143, -74.117)));
    private final AtomicInteger llamadasALaFuente = new AtomicInteger();

    @Test
    @DisplayName("arranque fallido: 503 hasta que el reintento lee la fuente; luego la caché responde con su hora")
    void arranqueFallidoYLuegoExitoso() {
        WeatherAlertPort caidaAlArrancar = zonas -> {
            if (llamadasALaFuente.incrementAndGet() == 1) {
                throw new FuenteAlertasNoDisponibleException("SIN_CONEXION");
            }
            return ResultadoFuenteAlertas.completo(List.of());
        };

        assertThat(actualizador(caidaAlArrancar, ARRANQUE).actualizar()).as("lectura del arranque").isFalse();
        assertThatThrownBy(() -> consultas().deZona(MINCA)).isInstanceOf(AlertasNoDisponiblesException.class);

        assertThat(actualizador(caidaAlArrancar, REINTENTO).reintentar()).isEqualTo(Reintento.ACTUALIZADA);
        assertThat(consultas().deZona(MINCA).actualizadoEn()).isEqualTo(REINTENTO);
        assertThat(llamadasALaFuente.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("sin lectura y la fuente sigue caída: FALLIDA, la caché sigue vacía y cada reintento vuelve a llamar")
    void reintentoFallido() {
        WeatherAlertPort caida = zonas -> {
            llamadasALaFuente.incrementAndGet();
            throw new FuenteAlertasNoDisponibleException("TIMEOUT");
        };

        assertThat(actualizador(caida, REINTENTO).reintentar()).isEqualTo(Reintento.FALLIDA);
        assertThat(actualizador(caida, REINTENTO.plusSeconds(120)).reintentar()).isEqualTo(Reintento.FALLIDA);
        assertThat(cache.ultima()).isEmpty();
        assertThat(llamadasALaFuente.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("con una lectura buena en la caché, el reintento no llama a la fuente ni toca la lectura")
    void conCacheLlenaNoLlama() {
        WeatherAlertPort fuente = zonas -> {
            llamadasALaFuente.incrementAndGet();
            return ResultadoFuenteAlertas.completo(List.of());
        };
        actualizador(fuente, ARRANQUE).actualizar();

        assertThat(actualizador(fuente, REINTENTO).reintentar()).isEqualTo(Reintento.NO_HACE_FALTA);
        assertThat(llamadasALaFuente.get()).isEqualTo(1);
        assertThat(cache.ultima()).hasValueSatisfying(
                lectura -> assertThat(lectura.actualizadoEn()).isEqualTo(ARRANQUE));
    }

    @Test
    @DisplayName("con el circuito del IDEAM abierto, el reintento cuenta como fallido sin llamar al IDEAM")
    void circuitoAbierto() {
        RestClient.Builder clientes = RestClient.builder().baseUrl("https://ideam.prueba/arcgis/MapServer/2");
        MockRestServiceServer ideam = MockRestServiceServer.bindTo(clientes).build();
        CircuitBreaker abierto = CircuitBreaker.ofDefaults("alertas");
        abierto.transitionToOpenState();

        Reintento resultado = actualizador(new IdeamAlertasHidrologicasAdapter(clientes.build(), abierto), REINTENTO)
                .reintentar();

        assertThat(resultado).isEqualTo(Reintento.FALLIDA);
        assertThat(cache.ultima()).isEmpty();
        ideam.verify();  // sin expectativas: cualquier llamada al IDEAM haría fallar la prueba
    }

    private ActualizarAlertasService actualizador(WeatherAlertPort fuente, Instant ahora) {
        return new ActualizarAlertasService(fuente, cache, catalogo, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    private ConsultarAlertasService consultas() {
        return new ConsultarAlertasService(cache, catalogo, Clock.fixed(REINTENTO, ZoneOffset.UTC));
    }
}
