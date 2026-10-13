package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarZonasUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ZonaController.class)
class ZonaControllerTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ConsultarZonasUseCase consultarZonas;

    @Test
    @DisplayName("devuelve las zonas activas con sus marcas; los campos sin valor no se envían")
    void zonasActivas() throws Exception {
        when(consultarZonas.consultarActivas()).thenReturn(List.of(zonaActiva(),
                new Zona("taganga", "Taganga", null, null, false, true, true, List.of(), null)));

        http.perform(get("/zonas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(ZONA_ACTIVA))
                .andExpect(jsonPath("$[0].areaProtegida").value(true))
                .andExpect(jsonPath("$[0].altaAfluencia").value(true))
                .andExpect(jsonPath("$[0].ubicacion.latitud").value(11.30))
                .andExpect(jsonPath("$[0].condicionesVisita").value("Ingreso hasta las 16:00"))
                .andExpect(jsonPath("$[1].nombre").value("Taganga"))
                .andExpect(jsonPath("$[1].descripcion").doesNotExist())
                .andExpect(jsonPath("$[1].ubicacion").doesNotExist());
    }
}
