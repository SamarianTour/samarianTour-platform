package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ContactoTurista;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Los adaptadores provisionales de prestadores y contactos (DN-15). */
class DatosDemoTest {

    private static final UUID AGENCIA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SIN_OPERADOR = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID OPERADOR = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID LAURA = UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID SIN_WHATSAPP = UUID.fromString("90000000-0000-0000-0000-000000000004");

    private final PropiedadesDemo demo = new PropiedadesDemo(
            List.of(new PropiedadesDemo.Prestador(AGENCIA, OPERADOR, "+573001110001"),
                    new PropiedadesDemo.Prestador(SIN_OPERADOR, null, "+573001110005")),
            List.of(new PropiedadesDemo.Turista(LAURA, "Laura Gómez", "+573105550001"),
                    new PropiedadesDemo.Turista(SIN_WHATSAPP, "Sin número", " ")));

    @Test
    @DisplayName("prestadores: el operador asignado y el WhatsApp; sin operador o desconocido, vacío")
    void prestadores() {
        PrestadoresDemoAdapter prestadores = new PrestadoresDemoAdapter(demo);

        assertThat(prestadores.operadorAsignado(AGENCIA)).contains(OPERADOR);
        assertThat(prestadores.whatsapp(AGENCIA)).contains("+573001110001");
        assertThat(prestadores.operadorAsignado(SIN_OPERADOR)).isEmpty();
        assertThat(prestadores.operadorAsignado(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("turistas: solo con WhatsApp; uno sin número cuenta como no verificado")
    void turistas() {
        ContactoDemoAdapter contactos = new ContactoDemoAdapter(demo);

        assertThat(contactos.turista(LAURA)).contains(new ContactoTurista("Laura Gómez", "+573105550001"));
        assertThat(contactos.turista(SIN_WHATSAPP)).isEmpty();
        assertThat(contactos.turista(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("sin el perfil demo la tabla queda vacía: nadie tiene operador ni contacto")
    void vacia() {
        PropiedadesDemo sinDemo = new PropiedadesDemo(null, null);

        assertThat(new PrestadoresDemoAdapter(sinDemo).operadorAsignado(AGENCIA)).isEmpty();
        assertThat(new ContactoDemoAdapter(sinDemo).turista(LAURA)).isEmpty();
    }
}
