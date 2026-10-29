package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.WhatsappNoConfiguradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.HasherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El WhatsApp del prestador y su verificación por código (RF-71, DN-33). El generador siempre da "048213". */
class ConfigurarWhatsAppPrestadorServiceTest {

    private static final String CODIGO = "048213";
    private static final HasherPort HASHER = new HasherPort() {
        @Override
        public String hash(String valor) {
            return "h:" + valor;
        }

        @Override
        public boolean coincide(String valor, String hash) {
            return hash.equals("h:" + valor);
        }
    };

    private final EscenarioPrestadores escenario = new EscenarioPrestadores();
    private Instant ahora = AHORA;
    private final ConfigurarWhatsAppPrestadorService servicio = new ConfigurarWhatsAppPrestadorService(
            escenario.repositorio, HASHER, escenario.whatsapp,
            new RandomGenerator() {
                @Override
                public long nextLong() {
                    return 48213L;
                }

                @Override
                public int nextInt(int limite) {
                    return 48213;
                }
            }, new Clock() {
                @Override
                public ZoneId getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(ZoneId zona) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return ahora;
                }
            });
    private UUID agencia;

    @BeforeEach
    void registrar() {
        escenario.rnt.put("910001", agenciaActiva("910001"));
        agencia = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(agencia, "910001", AGENCIA));
    }

    @Test
    @DisplayName("configurar el número envía el código; con él queda verificado y ya se le puede notificar")
    void verificar() {
        Prestador prestador = servicio.configurar(agencia, "+57 300 111 0101");
        assertThat(prestador.whatsappContactable()).isNull();
        assertThat(escenario.codigos).containsExactly("+573001110101: " + CODIGO);

        assertThat(servicio.verificar(agencia, CODIGO).whatsappContactable()).isEqualTo("+573001110101");
        assertThat(servicio.verificar(agencia, "000000").whatsappVerificado()).isTrue();
        servicio.enviarCodigo(agencia);
        assertThat(escenario.codigos).hasSize(1);
    }

    @Test
    @DisplayName("tres códigos equivocados lo invalidan; uno nuevo sirve; uno de más de 30 minutos, no")
    void intentosYVencimiento() {
        servicio.configurar(agencia, "+573001110101");
        for (int restantes = 2; restantes >= 0; restantes--) {
            int esperado = restantes;
            assertThatThrownBy(() -> servicio.verificar(agencia, "111111"))
                    .isInstanceOf(CodigoIncorrectoException.class)
                    .extracting(e -> ((CodigoIncorrectoException) e).intentosRestantes()).isEqualTo(esperado);
        }
        assertThatThrownBy(() -> servicio.verificar(agencia, CODIGO)).isInstanceOf(CodigoNoVigenteException.class);

        servicio.enviarCodigo(agencia);
        ahora = AHORA.plus(Duration.ofMinutes(30));
        assertThatThrownBy(() -> servicio.verificar(agencia, CODIGO)).isInstanceOf(CodigoNoVigenteException.class);
    }

    @Test
    @DisplayName("pedir un código sin número configurado responde WHATSAPP_NO_CONFIGURADO")
    void sinNumero() {
        assertThatThrownBy(() -> servicio.enviarCodigo(agencia)).isInstanceOf(WhatsappNoConfiguradoException.class);
    }
}
