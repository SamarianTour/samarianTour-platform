package co.edu.unimagdalena.samariantour.nucleo;

import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.CambioComisionResponse;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.ComisionRequest;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.ComisionResponse;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.DecisionModeracionResponse;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.DecisionOfertaRequest;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.DecisionOpinionRequest;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.PagoTardioResponse;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.CheckoutResponse;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.EventoRecibidoResponse;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.IntentoPagoResponse;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.SesionCobroResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.AtencionRequest;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.AvisoResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.CondicionesResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.EnviarSolicitudRequest;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.OfertaSolicitadaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.RespuestaCondicionesRequest;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudColaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudOperadorResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudResumenResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudTuristaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.TransicionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * El contrato del núcleo y sus controladores, como {@code ContratoCatalogoTest} en el catálogo: si una ruta existe en
 * un lado y no en el otro, falla, y cada esquema debe tener los mismos campos que su DTO. Es de integración porque
 * arranca el servicio completo, con su base. Cada módulo agrega sus DTO a {@link #esquemasConSuDto()}.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@ActiveProfiles("demo")
class ContratoNucleoIT extends IntegracionPostgresBase {

    /** La prueba corre con el módulo como directorio de trabajo; el contrato está en la raíz del repositorio. */
    private static final Path CONTRATO = Path.of("..", "..", "contratos", "nucleo.openapi.yaml");
    private static final Set<String> METODOS = Set.of("get", "post", "put", "patch", "delete");
    private static final String PAQUETE_BASE = "co.edu.unimagdalena.samariantour.nucleo.";
    private static final String CONTROLADORES = ".infraestructura.entrada.rest";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rutas;

    @Test
    @DisplayName("los controladores implementan exactamente las rutas del contrato OpenAPI")
    void rutasDelContrato() throws IOException {
        assertThat(rutasDeLosControladores()).isEqualTo(rutasDelContratoOpenApi());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("esquemasConSuDto")
    @DisplayName("cada esquema del contrato tiene los mismos campos que su DTO")
    void camposDelContrato(String esquema, Class<? extends Record> dto) throws IOException {
        assertThat(camposDe(dto)).as("campos de %s", dto.getSimpleName()).isEqualTo(camposDelEsquema(esquema));
    }

    static Stream<Arguments> esquemasConSuDto() {
        return Stream.of(
                arguments("DecisionOfertaRequest", DecisionOfertaRequest.class),
                arguments("DecisionOpinionRequest", DecisionOpinionRequest.class),
                arguments("DecisionModeracion", DecisionModeracionResponse.class),
                arguments("EnviarSolicitudRequest", EnviarSolicitudRequest.class),
                arguments("RespuestaCondicionesRequest", RespuestaCondicionesRequest.class),
                arguments("AtencionRequest", AtencionRequest.class),
                arguments("OfertaSolicitada", OfertaSolicitadaResponse.class),
                arguments("Condiciones", CondicionesResponse.class),
                arguments("Transicion", TransicionResponse.class),
                arguments("SolicitudResumen", SolicitudResumenResponse.class),
                arguments("SolicitudTurista", SolicitudTuristaResponse.class),
                arguments("SolicitudCola", SolicitudColaResponse.class),
                arguments("SolicitudOperador", SolicitudOperadorResponse.class),
                arguments("Aviso", AvisoResponse.class),
                arguments("PaginaSolicitudesTurista", PaginaResponse.class),
                arguments("PaginaColaOperador", PaginaResponse.class),
                arguments("PaginaAvisos", PaginaResponse.class),
                arguments("SesionCobro", SesionCobroResponse.class),
                arguments("Checkout", CheckoutResponse.class),
                arguments("IntentoPago", IntentoPagoResponse.class),
                arguments("EventoRecibido", EventoRecibidoResponse.class),
                arguments("ComisionRequest", ComisionRequest.class),
                arguments("Comision", ComisionResponse.class),
                arguments("CambioComision", CambioComisionResponse.class),
                arguments("PagoTardio", PagoTardioResponse.class));
    }

    private Set<String> rutasDeLosControladores() {
        Set<String> resultado = new TreeSet<>();
        rutas.getHandlerMethods().forEach((RequestMappingInfo ruta, HandlerMethod metodo) -> {
            String paquete = metodo.getBeanType().getPackageName();
            if (paquete.startsWith(PAQUETE_BASE) && paquete.endsWith(CONTROLADORES)) {
                ruta.getMethodsCondition().getMethods().forEach(verbo -> ruta.getPatternValues()
                        .forEach(patron -> resultado.add(verbo + " " + patron)));
            }
        });
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> rutasDelContratoOpenApi() throws IOException {
        Set<String> resultado = new TreeSet<>();
        ((Map<String, Map<String, Object>>) contrato().get("paths")).forEach((ruta, operaciones) ->
                operaciones.keySet().stream().filter(METODOS::contains)
                        .forEach(verbo -> resultado.add(verbo.toUpperCase() + " " + ruta)));
        return resultado;
    }

    private static Set<String> camposDe(Class<? extends Record> dto) {
        return Arrays.stream(dto.getRecordComponents()).map(RecordComponent::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> camposDelEsquema(String esquema) throws IOException {
        Map<String, Object> componentes = (Map<String, Object>) contrato().get("components");
        Map<String, Map<String, Object>> esquemas = (Map<String, Map<String, Object>>) componentes.get("schemas");
        return new TreeSet<>(((Map<String, Object>) esquemas.get(esquema).get("properties")).keySet());
    }

    private static Map<String, Object> contrato() throws IOException {
        try (Reader lector = Files.newBufferedReader(CONTRATO, StandardCharsets.UTF_8)) {
            return new Yaml().load(lector);
        }
    }
}
