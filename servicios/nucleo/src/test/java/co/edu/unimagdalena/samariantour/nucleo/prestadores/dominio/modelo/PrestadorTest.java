package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.TipoNoCoincideConRntException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.GUIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.guiaActivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El prestador y su RNT (RF-62, RF-63, RF-68 a RF-71, RF-76, RF-77, DN-33). */
class PrestadorTest {

    @Nested
    @DisplayName("Registro (RF-62, RF-63, RF-70)")
    class Registro {

        @Test
        @DisplayName("con el RNT activo autocompleta razón social y municipio y queda habilitado, versión 1")
        void activo() {
            Prestador agencia = Prestador.registrar(UUID.randomUUID(), " 910001 ", AGENCIA, agenciaActiva("910001"),
                    AHORA);

            assertThat(agencia.numeroRnt()).isEqualTo("910001");
            assertThat(agencia.razonSocial()).isEqualTo("MAR Y SELVA EXPEDICIONES S.A.S.");
            assertThat(agencia.municipio()).isEqualTo("SANTA MARTA");
            assertThat(agencia.estadoRnt()).isEqualTo(EstadoRnt.ACTIVO);
            assertThat(agencia.ultimaSincronizacion()).isEqualTo(AHORA);
            assertThat(agencia.habilitadoEnCatalogo()).isTrue();
            assertThat(agencia.versionHabilitacion()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin el RNT, No encontrado; con la fuente caída, Verificación pendiente; ninguno habilitado")
        void noEncontradoOPendiente() {
            Prestador sinRnt = Prestador.registrar(UUID.randomUUID(), "1", AGENCIA, new ConsultaRnt.NoEncontrado(),
                    AHORA);
            Prestador pendiente = Prestador.registrar(UUID.randomUUID(), "2", AGENCIA,
                    new ConsultaRnt.NoDisponible("timeout"), AHORA);

            assertThat(sinRnt.estadoRnt()).isEqualTo(EstadoRnt.NO_ENCONTRADO);
            assertThat(pendiente.estadoRnt()).isEqualTo(EstadoRnt.VERIFICACION_PENDIENTE);
            assertThat(pendiente.ultimaSincronizacion()).isNull();
            assertThat(List.of(sinRnt, pendiente)).noneMatch(Prestador::estaHabilitado)
                    .allMatch(prestador -> prestador.versionHabilitacion() == 0);
        }

        @Test
        @DisplayName("el tipo coincide con la categoría del RNT; el guía, además, es persona natural (RF-70)")
        void coherencia() {
            assertThatThrownBy(() -> Prestador.registrar(UUID.randomUUID(), "910002", AGENCIA, guiaActivo("910002"),
                    AHORA)).isInstanceOf(TipoNoCoincideConRntException.class);
            assertThatThrownBy(() -> Prestador.registrar(UUID.randomUUID(), "910001", GUIA, agenciaActiva("910001"),
                    AHORA)).isInstanceOf(TipoNoCoincideConRntException.class);
            assertThatThrownBy(() -> Prestador.registrar(UUID.randomUUID(), "910003", GUIA,
                    new ConsultaRnt.Encontrado(new RegistroRnt("910003", "GUIAS S.A.S.", "SANTA MARTA",
                            "GUIAS DE TURISMO", "GUIA DE TURISMO", false, 3)), AHORA))
                    .isInstanceOf(TipoNoCoincideConRntException.class);
            assertThat(Prestador.registrar(UUID.randomUUID(), "910002", GUIA, guiaActivo("910002"), AHORA)
                    .categoria()).isEqualTo(CategoriaPrestador.GUIA_INDEPENDIENTE);
        }

        @Test
        @DisplayName("el número de RNT son solo dígitos")
        void numero() {
            assertThatThrownBy(() -> Prestador.registrar(UUID.randomUUID(), "RNT-12", AGENCIA,
                    new ConsultaRnt.NoEncontrado(), AHORA))
                    .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("numeroRnt:");
        }
    }

    @Nested
    @DisplayName("Sincronización (RF-76, RF-77)")
    class Sincronizacion {

        @Test
        @DisplayName("un RNT activo que desaparece queda Inactivo; si vuelve, se rehabilita con otra versión")
        void caeYVuelve() {
            Prestador agencia = Prestador.registrar(UUID.randomUUID(), "910001", AGENCIA, agenciaActiva("910001"),
                    AHORA);

            assertThat(agencia.sincronizar(new ConsultaRnt.NoEncontrado(), AHORA.plus(Duration.ofDays(7)))).isTrue();
            assertThat(agencia.estadoRnt()).isEqualTo(EstadoRnt.INACTIVO);
            assertThat(agencia.versionHabilitacion()).isEqualTo(2);
            assertThat(agencia.ultimaSincronizacion()).isEqualTo(AHORA.plus(Duration.ofDays(7)));

            assertThat(agencia.sincronizar(agenciaActiva("910001"), AHORA.plus(Duration.ofDays(14)))).isTrue();
            assertThat(agencia.estadoRnt()).isEqualTo(EstadoRnt.ACTIVO);
            assertThat(agencia.versionHabilitacion()).isEqualTo(3);
        }

        @Test
        @DisplayName("sin cambio no hay aviso; con la fuente caída conserva el último estado conocido (QAS-03)")
        void sinCambio() {
            Prestador agencia = Prestador.registrar(UUID.randomUUID(), "910001", AGENCIA, agenciaActiva("910001"),
                    AHORA);

            assertThat(agencia.sincronizar(agenciaActiva("910001"), AHORA.plusSeconds(60))).isFalse();
            assertThat(agencia.sincronizar(new ConsultaRnt.NoDisponible("timeout"), AHORA.plusSeconds(120)))
                    .isFalse();
            assertThat(agencia.estadoRnt()).isEqualTo(EstadoRnt.ACTIVO);
            assertThat(agencia.ultimaSincronizacion()).isEqualTo(AHORA.plusSeconds(60));
        }

        @Test
        @DisplayName("un número que nunca apareció sigue No encontrado, no Inactivo; si aparece, se habilita")
        void nuncaVisto() {
            Prestador nuevo = Prestador.registrar(UUID.randomUUID(), "910001", AGENCIA,
                    new ConsultaRnt.NoDisponible("timeout"), AHORA);

            assertThat(nuevo.sincronizar(new ConsultaRnt.NoEncontrado(), AHORA)).isFalse();
            assertThat(nuevo.estadoRnt()).isEqualTo(EstadoRnt.NO_ENCONTRADO);
            assertThat(nuevo.sincronizar(agenciaActiva("910001"), AHORA)).isTrue();
            assertThat(nuevo.versionHabilitacion()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("cambiar el tipo exige que coincida con el RNT ya conocido (RF-68)")
    void cambiarTipo() {
        Prestador agencia = Prestador.registrar(UUID.randomUUID(), "910001", AGENCIA, agenciaActiva("910001"), AHORA);

        agencia.cambiarTipo(new DatosTipo(TipoPrestador.AGENCIA_OPERADORA, 300, List.of(), List.of()));
        assertThat(agencia.categoria()).isEqualTo(CategoriaPrestador.GRANDE);
        assertThatThrownBy(() -> agencia.cambiarTipo(GUIA)).isInstanceOf(TipoNoCoincideConRntException.class);
    }

    @Test
    @DisplayName("un WhatsApp nuevo queda sin verificar y no se usa para notificar hasta verificarlo (RF-71)")
    void whatsapp() {
        Prestador agencia = Prestador.registrar(UUID.randomUUID(), "910001", AGENCIA, agenciaActiva("910001"), AHORA);

        assertThat(agencia.cambiarWhatsapp("+57 300 111 0101")).isTrue();
        assertThat(agencia.whatsappContactable()).isNull();
        agencia.verificarWhatsapp();
        assertThat(agencia.whatsappContactable()).isEqualTo("+573001110101");
        assertThat(agencia.cambiarWhatsapp("+573001110101")).isFalse();
        assertThatThrownBy(() -> agencia.cambiarWhatsapp("3001110101")).isInstanceOf(DatoInvalidoException.class);
    }
}
