package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.notificacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class WhatsappSimuladoAdapterTest {

    private static final UUID SOLICITUD = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000001");

    private final WhatsappSimuladoAdapter adaptador =
            new WhatsappSimuladoAdapter("http://localhost:8080/turista/solicitudes/{id}");

    @Test
    @DisplayName("enmascara el número: solo los últimos 4 dígitos")
    void enmascarar() {
        assertThat(WhatsappSimuladoAdapter.enmascarar("+573105550001")).isEqualTo("******0001");
        assertThat(WhatsappSimuladoAdapter.enmascarar("123")).isEqualTo("****");
    }

    @Test
    @DisplayName("el log lleva el destinatario enmascarado y el tipo; nunca el número ni los parámetros")
    void log(CapturedOutput salida) {
        adaptador.enviar("+573001110001", TipoNotificacion.SOLICITUD_PAGADA, SOLICITUD,
                Map.of("whatsappTurista", "+573105550001", "nombreTurista", "Laura Gómez"));

        assertThat(salida.getOut()).contains("[WhatsApp simulado]", "******0001", "SOLICITUD_PAGADA")
                .doesNotContain("+573001110001", "3105550001", "Laura");
    }

    @Test
    @DisplayName("la notificación de Atendida muestra el enlace directo a la confirmación (RF-45)")
    void enlaceDeAtendida(CapturedOutput salida) {
        adaptador.enviar("+573105550001", TipoNotificacion.SOLICITUD_ATENDIDA, SOLICITUD, Map.of());

        assertThat(salida.getOut()).contains("enlace=http://localhost:8080/turista/solicitudes/" + SOLICITUD);
    }
}
