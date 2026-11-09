package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.ContrasenaRechazadaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorConCompromisosException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La supresión de los datos del prestador (RF-13, DN-45). */
class SuprimirPrestadorServiceTest {

    private final EscenarioPrestadores escenario = new EscenarioPrestadores();
    private UUID agencia;

    @BeforeEach
    void registrar() {
        escenario.rnt.put("910001", agenciaActiva("910001"));
        agencia = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(agencia, "910001", AGENCIA));
        escenario.avisos.clear();
    }

    @Test
    @DisplayName("lo deshabilita, avisa al catálogo, audita y anonimiza la cuenta; su RNT queda libre")
    void suprime() {
        escenario.suprimir.suprimir(agencia, EscenarioPrestadores.CONTRASENA);

        assertThat(escenario.repositorio.porId(agencia)).isEmpty();
        assertThat(escenario.prestadores.get(agencia).suprimidoEn()).isNotNull();
        assertThat(escenario.avisos).containsExactly(agencia + " false v2", agencia + " suprimido");
        assertThat(escenario.auditoria).containsExactly(agencia + " PRESTADOR_SUPRIMIDO RF-13");
        assertThat(escenario.cuentasSuprimidas).containsExactly(agencia);

        UUID otra = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(otra, "910001", AGENCIA));
        assertThat(escenario.repositorio.porId(otra)).isPresent();
    }

    @Test
    @DisplayName("sin el RNT registrado también puede: se anonimiza la cuenta y el catálogo borra su perfil")
    void sinRnt() {
        UUID guia = escenario.cuenta();

        escenario.suprimir.suprimir(guia, EscenarioPrestadores.CONTRASENA);

        assertThat(escenario.avisos).containsExactly(guia + " suprimido");
        assertThat(escenario.auditoria).isEmpty();
        assertThat(escenario.cuentasSuprimidas).containsExactly(guia);
    }

    @Test
    @DisplayName("con compromisos pendientes se niega, los enumera y no cambia nada")
    void conCompromisos() {
        escenario.compromisos.put(agencia, List.of("1 solicitudes pagadas cuyo servicio no ha terminado",
                "2 dispersiones por recibir"));

        assertThatThrownBy(() -> escenario.suprimir.suprimir(agencia, EscenarioPrestadores.CONTRASENA))
                .isInstanceOfSatisfying(PrestadorConCompromisosException.class,
                        error -> assertThat(error.compromisos()).hasSize(2));

        assertThat(escenario.repositorio.porId(agencia)).isPresent();
        assertThat(escenario.avisos).isEmpty();
        assertThat(escenario.cuentasSuprimidas).isEmpty();
    }

    @Test
    @DisplayName("con la contraseña equivocada se niega antes de mirar los compromisos")
    void contrasenaIncorrecta() {
        escenario.compromisos.put(agencia, List.of("2 dispersiones por recibir"));

        assertThatThrownBy(() -> escenario.suprimir.suprimir(agencia, "otra"))
                .isInstanceOfSatisfying(ContrasenaRechazadaException.class,
                        error -> assertThat(error.codigo()).isEqualTo("CONTRASENA_INCORRECTA"));

        assertThat(escenario.repositorio.porId(agencia)).isPresent();
        assertThat(escenario.cuentasSuprimidas).isEmpty();
    }
}
