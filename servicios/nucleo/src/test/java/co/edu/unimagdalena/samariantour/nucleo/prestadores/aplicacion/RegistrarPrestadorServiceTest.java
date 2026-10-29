package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.TipoNoCoincideConRntException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.EstadoRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.GUIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.guiaActivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El registro del prestador con su RNT y el cambio de número (RF-62, RF-63, RF-70, DN-33). */
class RegistrarPrestadorServiceTest {

    private final EscenarioPrestadores escenario = new EscenarioPrestadores();

    @BeforeEach
    void rnt() {
        escenario.rnt.put("910001", agenciaActiva("910001"));
        escenario.rnt.put("910006", guiaActivo("910006"));
        escenario.rnt.put("910009", new ConsultaRnt.NoDisponible("timeout"));
    }

    @Test
    @DisplayName("con el RNT activo queda habilitado y avisa al catálogo con la versión 1")
    void activo() {
        UUID id = escenario.cuenta();

        Prestador guia = escenario.registrar.registrar(new RegistrarPrestadorCommand(id, "910006", GUIA));

        assertThat(guia.estadoRnt()).isEqualTo(EstadoRnt.ACTIVO);
        assertThat(escenario.prestadores).containsKey(id);
        assertThat(escenario.avisos).containsExactly(id + " true v1");
    }

    @Test
    @DisplayName("sin el RNT, o con la fuente caída, se registra sin habilitar y sin avisar")
    void sinHabilitar() {
        Prestador noEncontrado = escenario.registrar.registrar(new RegistrarPrestadorCommand(escenario.cuenta(),
                "910099", AGENCIA));
        Prestador pendiente = escenario.registrar.registrar(new RegistrarPrestadorCommand(escenario.cuenta(),
                "910009", AGENCIA));

        assertThat(noEncontrado.estadoRnt()).isEqualTo(EstadoRnt.NO_ENCONTRADO);
        assertThat(pendiente.estadoRnt()).isEqualTo(EstadoRnt.VERIFICACION_PENDIENTE);
        assertThat(escenario.avisos).isEmpty();
    }

    @Test
    @DisplayName("sin cuenta de prestador, ya registrado, RNT de otro o tipo que no coincide: no se registra")
    void rechazos() {
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarPrestadorCommand(UUID.randomUUID(),
                "910001", AGENCIA))).isInstanceOf(CuentaNoEncontradaException.class);

        UUID agencia = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(agencia, "910001", AGENCIA));
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarPrestadorCommand(agencia, "910001",
                AGENCIA))).isInstanceOf(PrestadorYaRegistradoException.class);
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarPrestadorCommand(escenario.cuenta(),
                "910001", AGENCIA))).isInstanceOf(RntYaRegistradoException.class);
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarPrestadorCommand(escenario.cuenta(),
                "910006", AGENCIA))).isInstanceOf(TipoNoCoincideConRntException.class);
    }

    @Test
    @DisplayName("cambiar a un RNT que no existe lo deshabilita y avisa al catálogo con la versión 2")
    void cambiarRnt() {
        UUID id = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(id, "910001", AGENCIA));

        Prestador cambiado = escenario.registrar.cambiarRnt(id, "910098");

        assertThat(cambiado.estadoRnt()).isEqualTo(EstadoRnt.NO_ENCONTRADO);
        assertThat(cambiado.razonSocial()).isNull();
        assertThat(escenario.avisos).containsExactly(id + " true v1", id + " false v2");
        assertThatThrownBy(() -> escenario.registrar.cambiarRnt(UUID.randomUUID(), "910001"))
                .isInstanceOf(PrestadorNoRegistradoException.class);
    }

    @Test
    @DisplayName("el tipo se cambia si coincide con el RNT; la consulta de uno sin registrar responde no registrado")
    void tipoYConsulta() {
        UUID id = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(id, "910006", GUIA));

        assertThatThrownBy(() -> escenario.gestionar.actualizar(id, AGENCIA))
                .isInstanceOf(TipoNoCoincideConRntException.class);
        assertThat(escenario.gestionar.consultar(id).tipo()).isEqualTo(GUIA.tipo());
        assertThatThrownBy(() -> escenario.gestionar.consultar(UUID.randomUUID()))
                .isInstanceOf(PrestadorNoRegistradoException.class);
    }
}
