package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.rnt;

import co.edu.unimagdalena.samariantour.nucleo.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.RegistroRnt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Las dos fuentes del RNT (DN-33): la API SODA de datos.gov.co, contra un servidor simulado, y la tabla de la demo. */
class RntAdaptersTest {

    private static final CatalogoSimulado SODA = new CatalogoSimulado();
    private static final String CONJUNTO = "/resource/thwd-ivmp.json";

    private final RntSodaAdapter soda = new RntSodaAdapter(SODA.url() + CONJUNTO, "");

    @BeforeEach
    void reiniciar() {
        SODA.reiniciar();
    }

    @AfterAll
    static void apagar() {
        SODA.close();
    }

    @Test
    @DisplayName("SODA: lee los campos del conjunto real; el guía es No Comerciante, o sea persona natural")
    void sodaEncontrado() {
        SODA.responder(CONJUNTO, 200, """
                [{"codigo_rnt":"7017","municipio":"SANTA MARTA","estado_rnt":"ACTIVO","tipo_prestador":"No Comerciante",
                  "razon_social_establecimiento":"ANDERSON ANDRES MAYO MANJARRES","categoria":"GUIAS DE TURISMO",
                  "sub_categoria":"GUIA DE TURISMO","num_emp1":"1","ano":"2026","periodo":"AGOSTO"}]""");

        assertThat(soda.consultar("7017")).isEqualTo(new ConsultaRnt.Encontrado(new RegistroRnt("7017",
                "ANDERSON ANDRES MAYO MANJARRES", "SANTA MARTA", "GUIAS DE TURISMO", "GUIA DE TURISMO", true, 1)));
    }

    @Test
    @DisplayName("SODA: sin filas no se encuentra; un 5xx o más de 3 s es No disponible, nunca un error")
    void sodaNoEncontradoOCaido() {
        SODA.responder(CONJUNTO, 200, "[]");
        assertThat(soda.consultar("1")).isInstanceOf(ConsultaRnt.NoEncontrado.class);

        SODA.responder(CONJUNTO, 503, "{}");
        assertThat(soda.consultar("1")).isInstanceOf(ConsultaRnt.NoDisponible.class);

        SODA.responder(CONJUNTO, 200, "[]");
        SODA.demorar(Duration.ofSeconds(4));
        assertThat(soda.consultar("1")).isInstanceOf(ConsultaRnt.NoDisponible.class);
    }

    @Test
    @DisplayName("simulado: encuentra los números de su tabla, salvo los dados de baja")
    void simulado() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("910001"))).thenReturn(false);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("910002"))).thenReturn(true);
        RntSimuladoAdapter simulado = new RntSimuladoAdapter(new PropiedadesRntSimulado(List.of(
                new PropiedadesRntSimulado.Registro("910001", "MAR Y SELVA", "SANTA MARTA", "AGENCIAS DE VIAJES",
                        "AGENCIA DE VIAJES OPERADORAS", false, 12),
                new PropiedadesRntSimulado.Registro("910002", "JUAN CARLOS PEREZ ARIAS", "SANTA MARTA",
                        "GUIAS DE TURISMO", "GUIA DE TURISMO", true, 1))), jdbc);

        assertThat(simulado.consultar("910001")).isInstanceOf(ConsultaRnt.Encontrado.class);
        assertThat(simulado.consultar("910002")).isInstanceOf(ConsultaRnt.NoEncontrado.class);
        assertThat(simulado.consultar("910009")).isInstanceOf(ConsultaRnt.NoEncontrado.class);
        assertThat(simulado.retirar("910009")).isFalse();
    }
}
