package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.CatalogoDePrueba;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.AlertasNoDisponiblesException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertasDeZona;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoFuenteAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResumenAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.WeatherAlertPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.AlertaCacheCaffeineAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.MINCA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.TAGANGA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final Instant ANTES = AHORA.minus(Duration.ofHours(8));

    private final AlertaCacheCaffeineAdapter cache = new AlertaCacheCaffeineAdapter();
    private final AtomicInteger llamadasALaFuente = new AtomicInteger();
    private final List<ZonaCatalogo> zonas = List.of(
            new ZonaCatalogo(MINCA, "Minca y Sierra Nevada", 11.143, -74.117),
            new ZonaCatalogo(TAGANGA, "Taganga", 11.2675, -74.19));
    private final CatalogoConsultaPort catalogo = CatalogoDePrueba.de(List.of(), zonas);

    private final List<AlertaClimatica> alertas = List.of(
            alerta("minca-amarilla", MINCA, NivelAlerta.AMARILLA, AHORA.minus(Duration.ofHours(1)), null),
            alerta("minca-roja", MINCA, NivelAlerta.ROJA, AHORA.minus(Duration.ofHours(3)), null),
            alerta("minca-vencida", MINCA, NivelAlerta.ROJA, AHORA.minus(Duration.ofDays(2)), AHORA.minusSeconds(1)),
            alerta("taganga-hidrologica", TAGANGA, NivelAlerta.AMARILLA, null, null));

    @Test
    @DisplayName("actualizar consulta la fuente con las zonas del catálogo y guarda la lectura completa con su hora")
    void actualizar() {
        List<ZonaCatalogo> recibidas = new ArrayList<>();

        assertThat(actualizador(z -> {
            recibidas.addAll(z);
            return ResultadoFuenteAlertas.completo(alertas);
        }, ANTES).actualizar()).isTrue();

        assertThat(recibidas).isEqualTo(zonas);
        assertThat(cache.ultima()).hasValueSatisfying(lectura -> {
            assertThat(lectura.alertas()).hasSize(4);
            assertThat(lectura.actualizadoEn()).isEqualTo(ANTES);
        });
    }

    @Test
    @DisplayName("si la fuente falla del todo, la caché conserva la última lectura buena con su hora")
    void fuenteFalla() {
        actualizador(z -> ResultadoFuenteAlertas.completo(alertas), ANTES).actualizar();

        boolean actualizada = actualizador(z -> {
            throw new FuenteAlertasNoDisponibleException("TIMEOUT");
        }, AHORA).actualizar();

        assertThat(actualizada).isFalse();
        assertThat(cache.ultima()).hasValueSatisfying(lectura -> {
            assertThat(lectura.alertas()).hasSize(4);
            assertThat(lectura.actualizadoEn()).isEqualTo(ANTES);
        });
    }

    @Test
    @DisplayName("si falla una zona, conserva sus alertas anteriores con su hora; las demás quedan con la nueva lectura")
    void unaZonaFalla() {
        actualizador(z -> ResultadoFuenteAlertas.completo(alertas), ANTES).actualizar();
        AlertaClimatica nuevaDeMinca = alerta("minca-naranja", MINCA, NivelAlerta.NARANJA, null, null);

        actualizador(z -> new ResultadoFuenteAlertas(List.of(nuevaDeMinca), Set.of(TAGANGA)), AHORA).actualizar();

        AlertasDeZona taganga = consultas().deZona(TAGANGA);
        assertThat(taganga.alertas()).extracting(AlertaClimatica::id).containsExactly("taganga-hidrologica");
        assertThat(taganga.actualizadoEn()).isEqualTo(ANTES);
        AlertasDeZona minca = consultas().deZona(MINCA);
        assertThat(minca.alertas()).extracting(AlertaClimatica::id).containsExactly("minca-naranja");
        assertThat(minca.actualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("si el catálogo no responde, la fuente recibe la lista de zonas vacía")
    void sinCatalogo() {
        List<ZonaCatalogo> recibidas = new ArrayList<>(List.of(zonas.getFirst()));

        new ActualizarAlertasService(z -> {
            recibidas.clear();
            recibidas.addAll(z);
            return ResultadoFuenteAlertas.completo(List.of());
        }, cache, CatalogoDePrueba.caido(), Clock.fixed(AHORA, ZoneOffset.UTC)).actualizar();

        assertThat(recibidas).isEmpty();
    }

    @Test
    @DisplayName("sin ninguna lectura (la fuente nunca respondió), 503 ALERTAS_NO_DISPONIBLES")
    void sinCache() {
        actualizador(z -> {
            throw new FuenteAlertasNoDisponibleException("SIN_CONEXION");
        }, AHORA).actualizar();

        assertThatThrownBy(() -> consultas().deZona(MINCA)).isInstanceOf(AlertasNoDisponiblesException.class)
                .extracting(e -> ((AlertasNoDisponiblesException) e).codigo()).isEqualTo("ALERTAS_NO_DISPONIBLES");
        assertThatThrownBy(() -> consultas().todas()).isInstanceOf(AlertasNoDisponiblesException.class);
    }

    @Test
    @DisplayName("de una zona: solo las activas, de mayor a menor nivel; una zona sin alertas responde vacío")
    void deZona() {
        actualizador(z -> ResultadoFuenteAlertas.completo(alertas), ANTES).actualizar();

        AlertasDeZona minca = consultas().deZona(MINCA);

        assertThat(minca.actualizadoEn()).isEqualTo(ANTES);
        assertThat(minca.alertas()).extracting(AlertaClimatica::id).containsExactly("minca-roja", "minca-amarilla");
        assertThat(consultas().deZona("zona-que-no-existe").alertas()).isEmpty();
    }

    @Test
    @DisplayName("la consulta del turista nunca llama a la fuente: solo lee la caché")
    void nuncaLlamaALaFuente() {
        WeatherAlertPort fuente = z -> {
            llamadasALaFuente.incrementAndGet();
            return ResultadoFuenteAlertas.completo(alertas);
        };
        actualizador(fuente, ANTES).actualizar();

        consultas().deZona(MINCA);
        consultas().deZona(TAGANGA);
        consultas().todas();

        assertThat(llamadasALaFuente.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("todas: agrupadas por zona, con el nombre del catálogo o solo el id si no lo tiene")
    void todas() {
        List<AlertaClimatica> conOtraZona = new ArrayList<>(alertas);
        conOtraZona.add(alerta("sin-nombre", "a00000000000000000000099", NivelAlerta.AMARILLA, null, null));
        actualizador(z -> ResultadoFuenteAlertas.completo(conOtraZona), ANTES).actualizar();

        ResumenAlertas resumen = consultas().todas();

        assertThat(resumen.zonas()).extracting(ResumenAlertas.Zona::zonaId)
                .containsExactly(MINCA, TAGANGA, "a00000000000000000000099");
        assertThat(resumen.zonas()).extracting(ResumenAlertas.Zona::zonaNombre)
                .containsExactly("Minca y Sierra Nevada", "Taganga", null);
        assertThat(resumen.zonas().getFirst().alertas()).hasSize(2);
    }

    @Test
    @DisplayName("si el catálogo no responde, las alertas igual salen, sin nombres de zona")
    void todasSinCatalogo() {
        actualizador(z -> ResultadoFuenteAlertas.completo(alertas), ANTES).actualizar();

        ResumenAlertas resumen = new ConsultarAlertasService(cache, CatalogoDePrueba.caido(),
                Clock.fixed(AHORA, ZoneOffset.UTC)).todas();

        assertThat(resumen.zonas()).hasSize(2).allSatisfy(zona -> assertThat(zona.zonaNombre()).isNull());
    }

    private ActualizarAlertasService actualizador(WeatherAlertPort fuente, Instant ahora) {
        return new ActualizarAlertasService(fuente, cache, catalogo, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    private ConsultarAlertasService consultas() {
        return new ConsultarAlertasService(cache, catalogo, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static AlertaClimatica alerta(String id, String zonaId, NivelAlerta nivel, Instant emitida, Instant hasta) {
        return new AlertaClimatica(id, zonaId, TipoAlerta.LLUVIAS, nivel, "Lluvias", "Descripción", "IDEAM (simulado)",
                emitida, hasta);
    }
}
