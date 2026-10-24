package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El aviso de la bandeja de salida y sus estados (DN-06). */
class AvisoSalienteTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final String CUERPO = "{\"idAviso\":\"a-1\",\"tipo\":\"SUPRESION_TURISTA\"}";

    @Test
    @DisplayName("nace pendiente y se puede enviar de inmediato")
    void nuevo() {
        AvisoSaliente aviso = AvisoSaliente.nuevo("a-1", Destino.OPINIONES, CUERPO, AHORA);

        assertThat(aviso.estado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(aviso.proximoIntento()).isEqualTo(AHORA);
        assertThat(aviso.intentos()).isZero();
    }

    @Test
    @DisplayName("exige el identificador, de hasta 200 caracteres, y el cuerpo")
    void validaciones() {
        assertThatThrownBy(() -> AvisoSaliente.nuevo(" ", Destino.OFERTAS, CUERPO, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AvisoSaliente.nuevo("a".repeat(201), Destino.OFERTAS, CUERPO, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AvisoSaliente.nuevo("a-1", Destino.OFERTAS, " ", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recibido queda enviado; rechazado queda rechazado con el motivo; ninguno vuelve a pendiente")
    void finales() {
        AvisoSaliente enviado = AvisoSaliente.nuevo("a-1", Destino.OFERTAS, CUERPO, AHORA);
        enviado.registrar(new ResultadoEnvio.Recibido(), AHORA.plusSeconds(1));
        assertThat(enviado.estado()).isEqualTo(EstadoAviso.ENVIADO);
        assertThat(enviado.enviadoEn()).isEqualTo(AHORA.plusSeconds(1));
        assertThat(enviado.intentos()).isEqualTo(1);

        AvisoSaliente rechazado = AvisoSaliente.nuevo("a-2", Destino.OFERTAS, CUERPO, AHORA);
        rechazado.registrar(new ResultadoEnvio.Rechazado("400 DATO_INVALIDO"), AHORA);
        assertThat(rechazado.estado()).isEqualTo(EstadoAviso.RECHAZADO);
        assertThat(rechazado.ultimoError()).isEqualTo("400 DATO_INVALIDO");

        assertThatThrownBy(() -> rechazado.registrar(new ResultadoEnvio.Recibido(), AHORA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("un fallo lo deja pendiente y lo reprograma con la espera creciente; el motivo largo se recorta")
    void fallido() {
        AvisoSaliente aviso = AvisoSaliente.nuevo("a-1", Destino.OFERTAS, CUERPO, AHORA);

        aviso.registrar(new ResultadoEnvio.Fallido("503"), AHORA);
        assertThat(aviso.estado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(aviso.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofSeconds(5)));

        aviso.registrar(new ResultadoEnvio.Fallido("x".repeat(2_000)), AHORA.plusSeconds(5));
        assertThat(aviso.intentos()).isEqualTo(2);
        assertThat(aviso.proximoIntento()).isEqualTo(AHORA.plusSeconds(5).plus(Duration.ofSeconds(10)));
        assertThat(aviso.ultimoError()).hasSize(AvisoSaliente.LONGITUD_MAXIMA_ERROR);
    }
}
