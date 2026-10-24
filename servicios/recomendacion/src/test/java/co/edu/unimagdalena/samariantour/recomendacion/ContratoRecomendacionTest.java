package co.edu.unimagdalena.samariantour.recomendacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.OfertaRecomendadaResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionRequest;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * API first: el controlador, los DTO y los enum del servicio coinciden con {@code contratos/recomendacion.openapi.yaml}.
 * Si una ruta, un campo o un valor existe en un lado y no en el otro, esta prueba falla.
 */
@SpringBootTest
class ContratoRecomendacionTest {

    /** La prueba corre con el módulo como directorio de trabajo; el contrato está en la raíz del repositorio. */
    private static final Path CONTRATO = Path.of("..", "..", "contratos", "recomendacion.openapi.yaml");
    private static final Set<String> METODOS = Set.of("get", "post", "put", "patch", "delete");
    private static final String PAQUETE_CONTROLADORES =
            "co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rutas;

    @Test
    @DisplayName("el controlador implementa exactamente las rutas del contrato OpenAPI")
    void rutasDelContrato() throws IOException {
        assertThat(rutasDelControlador()).isEqualTo(rutasDelContratoOpenApi());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("esquemasConSuDto")
    @DisplayName("cada esquema del contrato tiene los mismos campos que su DTO")
    void camposDelContrato(String esquema, Class<? extends Record> dto) throws IOException {
        assertThat(camposDe(dto)).as("campos de %s", dto.getSimpleName()).isEqualTo(camposDelEsquema(esquema));
    }

    static Stream<Arguments> esquemasConSuDto() {
        return Stream.of(
                arguments("ConsultaRecomendacion", RecomendacionRequest.class),
                arguments("Recomendaciones", RecomendacionesResponse.class),
                arguments("Recomendacion", RecomendacionResponse.class),
                arguments("OfertaRecomendada", OfertaRecomendadaResponse.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("esquemasConSuEnum")
    @DisplayName("cada enum del contrato tiene los mismos valores que el del dominio")
    void valoresDelContrato(String esquema, Class<? extends Enum<?>> tipo) throws IOException {
        assertThat(valoresDelEsquema(esquema)).isEqualTo(Arrays.stream(tipo.getEnumConstants()).map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new)));
    }

    static Stream<Arguments> esquemasConSuEnum() {
        return Stream.of(
                arguments("MotorRecomendacion", MotorRecomendacion.class),
                arguments("CriterioAfinidad", CriterioAfinidad.class),
                arguments("TipoOferta", TipoOferta.class),
                arguments("TipoActividad", TipoActividad.class),
                arguments("NivelEsfuerzo", NivelEsfuerzo.class));
    }

    private Set<String> rutasDelControlador() {
        Set<String> resultado = new TreeSet<>();
        rutas.getHandlerMethods().forEach((RequestMappingInfo ruta, org.springframework.web.method.HandlerMethod metodo) -> {
            if (PAQUETE_CONTROLADORES.equals(metodo.getBeanType().getPackageName())) {
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

    private static Set<String> camposDelEsquema(String esquema) throws IOException {
        return new TreeSet<>(((Map<?, ?>) esquema(esquema).get("properties")).keySet().stream()
                .map(String::valueOf).toList());
    }

    private static Set<String> valoresDelEsquema(String esquema) throws IOException {
        return new TreeSet<>(((List<?>) esquema(esquema).get("enum")).stream().map(String::valueOf).toList());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> esquema(String nombre) throws IOException {
        Map<String, Object> componentes = (Map<String, Object>) contrato().get("components");
        return ((Map<String, Map<String, Object>>) componentes.get("schemas")).get(nombre);
    }

    private static Map<String, Object> contrato() throws IOException {
        try (Reader lector = Files.newBufferedReader(CONTRATO, StandardCharsets.UTF_8)) {
            return new Yaml().load(lector);
        }
    }
}
