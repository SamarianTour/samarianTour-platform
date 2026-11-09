package co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.excepcion.AvisoNoReenviableException;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.EstadoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.ResultadoEnvio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los rechazados, su reenvío y la limpieza de los enviados (DN-44). */
class AdministrarBandejaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:00:00Z");

    private final BandejaEnMemoria bandeja = new BandejaEnMemoria();
    private final AdministrarBandejaService servicio = new AdministrarBandejaService(bandeja,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    @DisplayName("lista los rechazados del más reciente al más antiguo y reenvía uno: queda pendiente para ya")
    void rechazadosYReenvio() {
        aviso("viejo", AHORA.minusSeconds(120), new ResultadoEnvio.Rechazado("400 viejo"));
        aviso("nuevo", AHORA.minusSeconds(60), new ResultadoEnvio.Rechazado("400 nuevo"));
        aviso("enviado", AHORA.minusSeconds(30), new ResultadoEnvio.Recibido());

        assertThat(servicio.rechazados()).extracting(AvisoSaliente::idAviso).containsExactly("nuevo", "viejo");

        AvisoSaliente reenviado = servicio.reenviar("viejo");

        assertThat(reenviado.estado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(reenviado.proximoIntento()).isEqualTo(AHORA);
        assertThat(bandeja.tomarPendientes(AHORA, 10)).extracting(AvisoSaliente::idAviso).containsExactly("viejo");
    }

    @Test
    @DisplayName("uno que no existe o que no está rechazado no se reenvía")
    void noReenviable() {
        aviso("enviado", AHORA, new ResultadoEnvio.Recibido());

        assertThatThrownBy(() -> servicio.reenviar("no-existe"))
                .isInstanceOfSatisfying(AvisoNoReenviableException.class,
                        e -> assertThat(e.codigo()).isEqualTo(AvisoNoReenviableException.NO_ENCONTRADO));
        assertThatThrownBy(() -> servicio.reenviar("enviado"))
                .isInstanceOfSatisfying(AvisoNoReenviableException.class,
                        e -> assertThat(e.codigo()).isEqualTo(AvisoNoReenviableException.NO_RECHAZADO));
    }

    @Test
    @DisplayName("la limpieza borra solo los enviados hace más de 30 días; los rechazados y pendientes se quedan")
    void limpieza() {
        Instant hace31Dias = AHORA.minus(Duration.ofDays(31));
        aviso("enviado-viejo", hace31Dias, new ResultadoEnvio.Recibido());
        aviso("enviado-reciente", AHORA.minus(Duration.ofDays(29)), new ResultadoEnvio.Recibido());
        aviso("rechazado-viejo", hace31Dias, new ResultadoEnvio.Rechazado("400"));
        bandeja.agregar(AvisoSaliente.nuevo("pendiente-viejo", Destino.OFERTAS, "{}", hace31Dias));

        assertThat(servicio.limpiar()).isEqualTo(1);

        assertThat(bandeja.buscar("enviado-viejo")).isEmpty();
        assertThat(bandeja.buscar("enviado-reciente")).isPresent();
        assertThat(bandeja.buscar("rechazado-viejo")).isPresent();
        assertThat(bandeja.buscar("pendiente-viejo")).isPresent();
    }

    private void aviso(String id, Instant cuando, ResultadoEnvio resultado) {
        AvisoSaliente aviso = AvisoSaliente.nuevo(id, Destino.OFERTAS, "{\"tipo\":\"MODERACION\"}", cuando);
        aviso.registrar(resultado, cuando);
        bandeja.agregar(aviso);
    }
}
