package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.CalculadoraComision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DispersionTest {

    private static final Instant AHORA = Instant.parse("2026-10-27T15:00:00Z");
    private static final Duration BASE = Duration.ofMinutes(1);

    private static Dispersion pendiente() {
        return Dispersion.pendiente(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                CalculadoraComision.liquidar(300_000, new BigDecimal("4.00"), CostoTransaccional.ninguno()), AHORA);
    }

    @Test
    @DisplayName("nace pendiente para ejecutarse ya, con la liquidación del pago confirmado (RF-53)")
    void nueva() {
        Dispersion dispersion = pendiente();

        assertThat(dispersion.estado()).isEqualTo(EstadoDispersion.PENDIENTE);
        assertThat(dispersion.proximoIntento()).isEqualTo(AHORA);
        assertThat(dispersion.montoNeto()).isEqualTo(288_000);
        assertThat(dispersion.intentos()).isZero();
    }

    @Test
    @DisplayName("exitosa: guarda la cuenta enmascarada y el id de la pasarela (RF-54, RF-75)")
    void exitosa() {
        Dispersion dispersion = pendiente();

        dispersion.registrarExito("******0101", "DSP-1", AHORA.plusSeconds(5));

        assertThat(dispersion.estado()).isEqualTo(EstadoDispersion.EXITOSA);
        assertThat(dispersion.cuentaDestino()).isEqualTo("******0101");
        assertThat(dispersion.idPasarela()).isEqualTo("DSP-1");
        assertThat(dispersion.intentos()).isEqualTo(1);
        assertThat(dispersion.proximoIntento()).isNull();
        assertThatThrownBy(() -> dispersion.registrarFallo("x", null, AHORA, BASE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("falla: tres reintentos con espera de 1, 2 y 4 min; al cuarto fallo queda fallida (RF-54)")
    void reintentos() {
        Dispersion dispersion = pendiente();

        dispersion.registrarFallo("rechazada", "******0404", AHORA, BASE);
        assertThat(dispersion.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(1)));
        dispersion.registrarFallo("rechazada", "******0404", AHORA, BASE);
        assertThat(dispersion.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(2)));
        dispersion.registrarFallo("rechazada", "******0404", AHORA, BASE);
        assertThat(dispersion.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(4)));
        assertThat(dispersion.estado()).isEqualTo(EstadoDispersion.PENDIENTE);

        dispersion.registrarFallo("rechazada", "******0404", AHORA, BASE);

        assertThat(dispersion.estado()).isEqualTo(EstadoDispersion.FALLIDA);
        assertThat(dispersion.intentos()).isEqualTo(4);
        assertThat(dispersion.ultimaCausa()).isEqualTo("rechazada");
        assertThat(dispersion.proximoIntento()).isNull();
        assertThat(dispersion.avisoEnviadoEn()).isNull();
    }

    @Test
    @DisplayName("la cuenta de cobro solo se muestra con sus últimos 4 dígitos, también en toString (RF-75)")
    void cuentaEnmascarada() {
        CuentaCobro cuenta = new CuentaCobro(TipoCuenta.AHORROS, "Bancolombia", "40112233440101");

        assertThat(cuenta.enmascarada()).isEqualTo("******0101");
        assertThat(cuenta.toString()).doesNotContain("4011223344").contains("******0101");
        assertThat(new CuentaCobro(TipoCuenta.NEQUI, "Nequi", "12").enmascarada()).isEqualTo("****");
    }
}
