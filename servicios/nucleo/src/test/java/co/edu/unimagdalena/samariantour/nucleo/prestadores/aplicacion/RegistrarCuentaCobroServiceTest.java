package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaCobroNoRegistradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentaCobroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La cuenta de cobro con su historial y su auditoría (RF-74, RF-75, DN-34). */
class RegistrarCuentaCobroServiceTest {

    private final EscenarioPrestadores escenario = new EscenarioPrestadores();
    /** El historial: la vigente es la que no tiene fecha de cierre. */
    private final List<CuentaCobro> historial = new ArrayList<>();
    private final List<String> auditoria = new ArrayList<>();
    private final RegistrarCuentaCobroService servicio = new RegistrarCuentaCobroService(escenario.repositorio,
            new CuentaCobroRepositoryPort() {
                @Override
                public Optional<CuentaCobro> vigente(UUID prestadorId) {
                    return historial.stream().filter(c -> c.prestadorId().equals(prestadorId) && c.esVigente())
                            .findFirst();
                }

                @Override
                public void reemplazar(CuentaCobro anterior, CuentaCobro nueva) {
                    if (anterior != null) {
                        historial.replaceAll(c -> c.id().equals(anterior.id()) ? anterior : c);
                    }
                    historial.add(nueva);
                }
            }, (prestadorId, accion, detalle, cuando) -> auditoria.add(accion + " " + detalle),
            Clock.fixed(AHORA, ZoneOffset.UTC));
    private UUID agencia;

    @BeforeEach
    void registrar() {
        escenario.rnt.put("910001", agenciaActiva("910001"));
        agencia = escenario.cuenta();
        escenario.registrar.registrar(new RegistrarPrestadorCommand(agencia, "910001", AGENCIA));
    }

    @Test
    @DisplayName("cambiar la cuenta cierra la anterior y la audita; los mismos datos no cambian nada")
    void historialYAuditoria() {
        servicio.registrar(agencia, TipoCuenta.AHORROS, " Bancolombia ", "4011-2233-440101", "Mar y Selva");
        servicio.registrar(agencia, TipoCuenta.AHORROS, "Bancolombia", "40112233440101", "Mar y Selva");
        CuentaCobro nequi = servicio.registrar(agencia, TipoCuenta.NEQUI, null, "300 111 0101", "Mar y Selva");

        assertThat(historial).hasSize(2);
        assertThat(historial.getFirst().vigenteHasta()).isEqualTo(AHORA);
        assertThat(servicio.vigente(agencia)).isEqualTo(nequi);
        assertThat(nequi.entidad()).isEqualTo("Nequi");
        assertThat(nequi.numero()).isEqualTo("3001110101");
        assertThat(auditoria).containsExactly("CUENTA_COBRO_REGISTRADA AHORROS Bancolombia ******0101",
                "CUENTA_COBRO_CAMBIADA NEQUI Nequi ******0101");
        assertThat(nequi.toString()).doesNotContain("3001110101");
    }

    @Test
    @DisplayName("el número de la billetera es el celular; el banco exige entidad; sin RNT registrado, no se puede")
    void reglas() {
        assertThatThrownBy(() -> servicio.registrar(agencia, TipoCuenta.DAVIPLATA, null, "40112233", "Mar y Selva"))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("numero:");
        assertThatThrownBy(() -> servicio.registrar(agencia, TipoCuenta.CORRIENTE, " ", "40112233", "Mar y Selva"))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("entidad:");
        assertThatThrownBy(() -> servicio.registrar(UUID.randomUUID(), TipoCuenta.NEQUI, null, "3001110101", "Ana"))
                .isInstanceOf(PrestadorNoRegistradoException.class);
        assertThatThrownBy(() -> servicio.vigente(agencia)).isInstanceOf(CuentaCobroNoRegistradaException.class);
    }
}
