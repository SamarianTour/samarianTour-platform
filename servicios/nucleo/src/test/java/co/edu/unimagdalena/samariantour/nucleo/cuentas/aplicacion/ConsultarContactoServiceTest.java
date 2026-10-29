package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.ContactoTurista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.modulos.CuentasApiInternaAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** El contacto del turista que las cuentas entregan a los demás módulos (RF-44, RF-57, DN-32). */
class ConsultarContactoServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();
    private final CuentasApiInternaAdapter api =
            new CuentasApiInternaAdapter(new ConsultarContactoService(escenario.repositorio));

    @Test
    @DisplayName("solo con el WhatsApp verificado y la cuenta activa; suprimida, ya no hay contacto")
    void soloVerificadoYActivo() {
        Usuario laura = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez",
                "laura@correo.co", "Playa2026", "+573001234567", true, "v1"));
        assertThat(api.contactoTurista(laura.id())).isEmpty();

        escenario.verificarWhatsApp.verificar(laura.id(), EscenarioCuentas.CODIGO);
        assertThat(api.contactoTurista(laura.id())).contains(new ContactoTurista("Laura Gómez", "+573001234567"));

        escenario.supresion.suprimir(laura.id(), "Playa2026");
        assertThat(api.contactoTurista(laura.id())).isEmpty();
    }

    @Test
    @DisplayName("un prestador no es un turista contactable")
    void prestador() {
        Usuario guia = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.PRESTADOR, "Guía",
                "guia@correo.co", "Minca2026", null, true, "v1"));

        assertThat(api.contactoTurista(guia.id())).isEmpty();
    }
}
