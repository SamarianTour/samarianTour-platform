package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.EstadoRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ResultadoSincronizacion;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static org.assertj.core.api.Assertions.assertThat;

/** La sincronización con el RNT (RF-76 a RF-78, DN-36). */
class SincronizarRntServiceTest {

    private final EscenarioPrestadores escenario = new EscenarioPrestadores();

    @Test
    @DisplayName("el RNT que desaparece deja al prestador Inactivo, lo retira del catálogo y le avisa por WhatsApp")
    void retiro() {
        UUID agencia = registrar("910001", "+573001110101");
        escenario.rnt.remove("910001");

        ResultadoSincronizacion resultado = sincronizar(AHORA.plus(Duration.ofDays(7)));

        assertThat(resultado).isEqualTo(new ResultadoSincronizacion(1, 1, 0, 0, false));
        assertThat(escenario.prestadores.get(agencia).estadoRnt()).isEqualTo(EstadoRnt.INACTIVO);
        assertThat(escenario.avisos).containsExactly(agencia + " true v1", agencia + " false v2");
        assertThat(escenario.retiros).containsExactly("+573001110101 910001");
    }

    @Test
    @DisplayName("si el RNT vuelve a estar activo, el catálogo lo restituye sin avisar al prestador")
    void restitucion() {
        UUID agencia = registrar("910001", "+573001110101");
        escenario.rnt.remove("910001");
        sincronizar(AHORA.plus(Duration.ofDays(7)));
        escenario.rnt.put("910001", agenciaActiva("910001"));

        ResultadoSincronizacion resultado = sincronizar(AHORA.plus(Duration.ofDays(14)));

        assertThat(resultado).isEqualTo(new ResultadoSincronizacion(1, 0, 1, 0, false));
        assertThat(escenario.prestadores.get(agencia).estadoRnt()).isEqualTo(EstadoRnt.ACTIVO);
        assertThat(escenario.prestadores.get(agencia).ultimaSincronizacion())
                .isEqualTo(AHORA.plus(Duration.ofDays(14)));
        assertThat(escenario.avisos).endsWith(agencia + " true v3");
        assertThat(escenario.retiros).hasSize(1);
    }

    @Test
    @DisplayName("sin cambios no hay avisos; sin WhatsApp verificado, el retiro no se notifica")
    void sinCambiosYSinWhatsapp() {
        registrar("910001", "+573001110101");
        UUID sinWhatsapp = registrar("910002", null);
        UUID nuncaVisto = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(nuncaVisto, "910099", AGENCIA));
        escenario.avisos.clear();
        escenario.rnt.remove("910002");

        ResultadoSincronizacion resultado = sincronizar(AHORA.plus(Duration.ofDays(7)));

        assertThat(resultado).isEqualTo(new ResultadoSincronizacion(3, 1, 0, 0, false));
        assertThat(escenario.avisos).containsExactly(sinWhatsapp + " false v2");
        assertThat(escenario.retiros).isEmpty();
        assertThat(escenario.prestadores.get(nuncaVisto).estadoRnt()).isEqualTo(EstadoRnt.NO_ENCONTRADO);
    }

    @Test
    @DisplayName("si el RNT no responde tres veces seguidas se detiene y cada prestador conserva su estado (QAS-03)")
    void rntCaido() {
        for (int i = 1; i <= 5; i++) {
            registrar("91000" + i, null);
        }
        escenario.avisos.clear();
        escenario.rnt.replaceAll((numero, consulta) -> new ConsultaRnt.NoDisponible("timeout"));

        ResultadoSincronizacion resultado = sincronizar(AHORA.plus(Duration.ofDays(7)));

        assertThat(resultado).isEqualTo(new ResultadoSincronizacion(3, 0, 0, 3, true));
        assertThat(escenario.prestadores.values()).extracting(Prestador::estadoRnt).containsOnly(EstadoRnt.ACTIVO);
        assertThat(escenario.prestadores.values()).extracting(Prestador::ultimaSincronizacion).containsOnly(AHORA);
        assertThat(escenario.avisos).isEmpty();
    }

    @Test
    @DisplayName("revisa a todos en lotes, y una sincronización nueva los vuelve a revisar")
    void lotes() {
        int total = SincronizarRntService.LOTE * 2 + 5;
        for (int i = 0; i < total; i++) {
            registrar(String.valueOf(920000 + i), null);
        }

        assertThat(sincronizar(AHORA.plus(Duration.ofDays(7))).revisados()).isEqualTo(total);
        assertThat(sincronizar(AHORA.plus(Duration.ofDays(14))).revisados()).isEqualTo(total);
    }

    /** Una agencia activa con su WhatsApp verificado, o sin él. */
    private UUID registrar(String numeroRnt, String whatsapp) {
        escenario.rnt.put(numeroRnt, agenciaActiva(numeroRnt));
        UUID id = escenario.cuenta();
        Prestador prestador = escenario.registrar.registrar(new RegistrarPrestadorCommand(id, numeroRnt, AGENCIA));
        if (whatsapp != null) {
            prestador.cambiarWhatsapp(whatsapp);
            prestador.verificarWhatsapp();
        }
        return id;
    }

    private ResultadoSincronizacion sincronizar(Instant ahora) {
        return new SincronizarRntService(escenario.repositorio, escenario.fuenteRnt, escenario.eventos,
                escenario.whatsapp, escenario.transaccion, Clock.fixed(ahora, ZoneOffset.UTC)).sincronizar();
    }
}
