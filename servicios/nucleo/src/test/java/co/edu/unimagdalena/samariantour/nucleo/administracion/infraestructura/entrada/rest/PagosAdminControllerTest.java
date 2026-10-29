package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.IdentidadPorHeaders;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.ComisionConfigurada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.ComisionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.PagoTardioRegistrado;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.PagosAdministracionApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La comisión y los pagos tardíos del administrador, por {@code pagos.api} (RF-50, RF-82, RF-83, DN-23). */
@WebMvcTest(PagosAdminController.class)
@Import(IdentidadPorHeaders.class)
@ActiveProfiles("demo")
class PagosAdminControllerTest {

    private static final UUID ADMINISTRADOR = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final Instant ENERO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant HOY = Instant.parse("2026-10-27T15:00:00Z");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private PagosAdministracionApi pagos;

    @Test
    @DisplayName("la comisión vigente es la primera del historial; el inicial no lleva administrador")
    void comision() throws Exception {
        when(pagos.historialComision()).thenReturn(List.of(new ComisionConfigurada(new BigDecimal("4.00"), ENERO,
                null)));

        http.perform(get("/admin/comision").header("X-Usuario-Id", ADMINISTRADOR).header("X-Rol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentaje").value(4.0))
                .andExpect(jsonPath("$.vigenteDesde").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.historial[0].administradorId").doesNotExist());
    }

    @Test
    @DisplayName("cambiarla: la pide a pagos con el administrador de la identidad; un porcentaje inválido es 400")
    void cambiar() throws Exception {
        when(pagos.historialComision()).thenReturn(List.of(
                new ComisionConfigurada(new BigDecimal("5.50"), HOY, ADMINISTRADOR),
                new ComisionConfigurada(new BigDecimal("4.00"), ENERO, null)));

        http.perform(put("/admin/comision").header("X-Usuario-Id", ADMINISTRADOR).header("X-Rol", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"porcentaje\": 5.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentaje").value(5.5))
                .andExpect(jsonPath("$.historial.length()").value(2));
        verify(pagos).actualizarComision(new BigDecimal("5.5"), ADMINISTRADOR);

        when(pagos.actualizarComision(any(), any()))
                .thenThrow(new ComisionInvalidaException("porcentaje: debe estar entre 0 y menos de 100"));
        http.perform(put("/admin/comision").header("X-Usuario-Id", ADMINISTRADOR).header("X-Rol", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"porcentaje\": 150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail").value("porcentaje: debe estar entre 0 y menos de 100"));
    }

    @Test
    @DisplayName("los pagos tardíos para el reembolso manual (RF-50)")
    void pagosTardios() throws Exception {
        UUID solicitud = UUID.randomUUID();
        when(pagos.pagosTardios()).thenReturn(List.of(new PagoTardioRegistrado(UUID.randomUUID(), solicitud,
                "ST-1", "T-1", 300_000, HOY, true)));

        http.perform(get("/admin/pagos-tardios").header("X-Usuario-Id", ADMINISTRADOR)
                        .header("X-Rol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].solicitudId").value(solicitud.toString()))
                .andExpect(jsonPath("$[0].monto").value(300_000))
                .andExpect(jsonPath("$[0].avisado").value(true));
    }

    @Test
    @DisplayName("solo el administrador: el operador no ve la comisión (SRS, perfil del operador)")
    void soloElAdministrador() throws Exception {
        http.perform(get("/admin/comision").header("X-Usuario-Id", UUID.randomUUID())
                .header("X-Rol", "OPERADOR_DISPONIBILIDAD")).andExpect(status().isForbidden());
        verifyNoInteractions(pagos);
    }
}
