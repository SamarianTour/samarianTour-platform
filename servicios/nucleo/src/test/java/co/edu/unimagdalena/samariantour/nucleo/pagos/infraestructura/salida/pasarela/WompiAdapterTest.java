package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OrdenDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.TipoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config.PropiedadesPagos;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.wompi.WompiAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El adaptador de Wompi sin llamar a Wompi: la sesión del Web Checkout y la dispersión delegada (DN-18). */
class WompiAdapterTest {

    private static final UUID SOLICITUD = UUID.fromString("6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20");

    private static PropiedadesPagos propiedades(String llave, String integridad, String eventos) {
        return new PropiedadesPagos("wompi", "http://localhost:8080/turista/solicitudes/{id}",
                new PropiedadesPagos.Costo(new BigDecimal("2.65"), 700, new BigDecimal("19")),
                new PropiedadesPagos.Dispersion(Duration.ofMinutes(1)),
                new PropiedadesPagos.Simulada("http://localhost:8080/api/nucleo",
                        "http://localhost:8080/api/nucleo/pagos/eventos", "demo", Duration.ofSeconds(3),
                        List.of("3001110404")),
                new PropiedadesPagos.Wompi(llave, integridad, eventos, "https://checkout.wompi.co/p/"));
    }

    @Test
    @DisplayName("la sesión es el Web Checkout con la llave pública, el monto en centavos y la firma de integridad")
    void sesion() {
        WompiAdapter wompi = new WompiAdapter(propiedades("pub_test_llave", "test_integrity_secreto",
                "test_events_secreto"), new ObjectMapper());
        IntentoPago intento = IntentoPago.nuevo(UUID.randomUUID(), SOLICITUD, UUID.randomUUID(), 1, 300_000,
                Instant.parse("2026-10-27T15:00:00Z"));

        SesionCobro sesion = wompi.crearSesion(intento);

        String firma = FormatoWompi.firmaIntegridad(intento.referencia(), 30_000_000, "COP",
                "test_integrity_secreto");
        Map<String, List<String>> parametros = UriComponentsBuilder.fromUriString(sesion.urlCheckout()).build()
                .getQueryParams();
        assertThat(sesion.pasarela()).isEqualTo("WOMPI");
        assertThat(sesion.urlCheckout()).startsWith("https://checkout.wompi.co/p/?");
        assertThat(parametros.get("public-key")).containsExactly("pub_test_llave");
        assertThat(parametros.get("currency")).containsExactly("COP");
        assertThat(parametros.get("amount-in-cents")).containsExactly("30000000");
        assertThat(parametros.get("reference")).containsExactly(intento.referencia());
        assertThat(parametros.get("signature:integrity")).containsExactly(firma);
        assertThat(sesion.llavePublica()).isEqualTo("pub_test_llave");
        assertThat(sesion.firmaIntegridad()).isEqualTo(firma);
        assertThat(sesion.urlRedireccion()).isEqualTo("http://localhost:8080/turista/solicitudes/" + SOLICITUD);
        assertThat(sesion.urlCheckout()).doesNotContain("test_integrity_secreto", "test_events_secreto");
    }

    @Test
    @DisplayName("sin las llaves de Wompi no arranca; la dispersión va a la simulada (Pagos a terceros sin activar)")
    void llavesYDispersion() {
        assertThatThrownBy(() -> new WompiAdapter(propiedades("", "x", "y"), new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("WOMPI_LLAVE_PUBLICA");
        WompiAdapter wompi = new WompiAdapter(propiedades("pub_test_llave", "test_integrity_secreto",
                "test_events_secreto"), new ObjectMapper());

        assertThat(wompi.dispersar(new OrdenDispersion(UUID.randomUUID(), UUID.randomUUID(), 277_706,
                new CuentaCobro(TipoCuenta.AHORROS, "Bancolombia", "40112233440101"))).exitosa()).isTrue();
        assertThat(wompi.dispersar(new OrdenDispersion(UUID.randomUUID(), UUID.randomUUID(), 277_706,
                new CuentaCobro(TipoCuenta.DAVIPLATA, "Daviplata", "3001110404"))).exitosa()).isFalse();
        assertThat(propiedades("pub_test_llave", "test_integrity_secreto", "test_events_secreto").wompi()
                .toString()).doesNotContain("secreto");
    }
}
