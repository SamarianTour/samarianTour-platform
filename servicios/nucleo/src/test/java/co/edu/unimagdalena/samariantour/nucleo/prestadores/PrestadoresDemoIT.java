package co.edu.unimagdalena.samariantour.nucleo.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ContactosPagosPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.CuentasCobroPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadoresApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los prestadores de la demo y lo que ven de ellos los demás módulos (DN-35): reemplazan las tablas provisionales de
 * solicitudes (DN-15) y de pagos (DN-26).
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PrestadoresDemoIT extends IntegracionPostgresBase {

    private static final UUID AGENCIA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID RESTAURANTE = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID SIN_RNT = UUID.fromString("50000000-0000-0000-0000-000000000005");

    @Autowired
    private MockMvc http;

    @Autowired
    private PrestadoresApiInterna api;

    @Autowired
    private PrestadoresPort solicitudes;

    @Autowired
    private CuentasCobroPort cuentasPagos;

    @Autowired
    private ContactosPagosPort contactosPagos;

    @Test
    @DisplayName("la agencia de la demo ve su RNT activo, su categoría y su cuenta enmascarada")
    void agencia() throws Exception {
        http.perform(get("/prestador/registro")
                        .header(HttpHeaders.AUTHORIZATION,
                                TokensDePrueba.bearerPrestador(AGENCIA.toString(), "AGENCIA_OPERADORA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRnt").value("ACTIVO"))
                .andExpect(jsonPath("$.razonSocial").value("MAR Y SELVA EXPEDICIONES S.A.S."))
                .andExpect(jsonPath("$.categoria").value("MICRO_PEQUENO"))
                .andExpect(jsonPath("$.whatsappVerificado").value(true));
        http.perform(get("/prestador/cuenta-cobro")
                        .header(HttpHeaders.AUTHORIZATION,
                                TokensDePrueba.bearerPrestador(AGENCIA.toString(), "AGENCIA_OPERADORA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroEnmascarado").value("******0101"));
    }

    @Test
    @DisplayName("solicitudes y pagos toman el operador, el WhatsApp y la cuenta del módulo de prestadores")
    void demasModulos() {
        assertThat(solicitudes.operadorAsignado(AGENCIA))
                .contains(UUID.fromString("60000000-0000-0000-0000-000000000001"));
        assertThat(solicitudes.whatsapp(AGENCIA)).contains("+573001110101");
        assertThat(contactosPagos.whatsappPrestador(RESTAURANTE)).contains("+573001110404");
        assertThat(cuentasPagos.cuentaVigente(RESTAURANTE)).hasValueSatisfying(cuenta -> {
            assertThat(cuenta.entidad()).isEqualTo("Daviplata");
            assertThat(cuenta.numero()).isEqualTo("3001110404");
        });
        assertThat(contactosPagos.whatsappAdministrador()).contains("+573005550099");

        assertThat(api.consultar(SIN_RNT)).hasValueSatisfying(prestador -> {
            assertThat(prestador.habilitado()).isFalse();
            assertThat(prestador.operadorId()).isNull();
        });
        assertThat(solicitudes.operadorAsignado(SIN_RNT)).isEmpty();
        assertThat(api.consultar(UUID.randomUUID())).isEmpty();
    }
}
