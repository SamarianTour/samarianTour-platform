package co.edu.unimagdalena.samariantour.catalogo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ActividadIncluidaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.AvisoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ContenidoVersionResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.CoordenadaDto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FichaOfertaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FotoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaEnModeracionResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaInternaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaPendienteResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaPrestadorResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaResumenResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaVisibleResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilPublicoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.VersionResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ZonaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.AvisoOpinionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.MiOpinionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionEnModeracionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionPendienteResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionPublicaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionRequest;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionesOfertaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.ResumenOpinionesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * API first: los controladores y los DTO del servicio, de los módulos catálogo y opiniones, coinciden con el contrato
 * {@code contratos/catalogo.openapi.yaml}. Si una ruta o un campo existe en un lado y no en el otro, esta prueba falla.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration")
@ActiveProfiles({"memoria", "demo"})
class ContratoCatalogoTest {

    /** La prueba corre con el módulo como directorio de trabajo; el contrato está en la raíz del repositorio. */
    private static final Path CONTRATO = Path.of("..", "..", "contratos", "catalogo.openapi.yaml");
    private static final Set<String> METODOS = Set.of("get", "post", "put", "patch", "delete");
    private static final Set<String> PAQUETES_CONTROLADORES = Set.of(
            "co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest",
            "co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest");

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
                arguments("Coordenada", CoordenadaDto.class),
                arguments("OfertaResumen", OfertaResumenResponse.class),
                arguments("FichaOferta", FichaOfertaResponse.class),
                arguments("DetalleActividad", FichaOfertaResponse.DetalleActividad.class),
                arguments("DetallePaquete", FichaOfertaResponse.DetallePaquete.class),
                arguments("DetalleAlojamiento", FichaOfertaResponse.DetalleAlojamiento.class),
                arguments("ActividadIncluida", ActividadIncluidaResponse.class),
                arguments("Zona", ZonaResponse.class),
                arguments("PerfilPublico", PerfilPublicoResponse.class),
                arguments("OfertaRequest", OfertaRequest.class),
                arguments("OfertaPrestador", OfertaPrestadorResponse.class),
                arguments("Version", VersionResponse.class),
                arguments("AvisoResultado", AvisoResponse.class),
                arguments("OfertaInterna", OfertaInternaResponse.class),
                arguments("OfertaVisible", OfertaVisibleResponse.class),
                arguments("ContenidoVersion", ContenidoVersionResponse.class),
                arguments("OfertaPendiente", OfertaPendienteResponse.class),
                arguments("OfertaEnModeracion", OfertaEnModeracionResponse.class),
                arguments("OpinionRequest", OpinionRequest.class),
                arguments("MiOpinion", MiOpinionResponse.class),
                arguments("OpinionPublica", OpinionPublicaResponse.class),
                arguments("ResumenOpiniones", ResumenOpinionesResponse.class),
                arguments("OpinionesOferta", OpinionesOfertaResponse.class),
                arguments("OpinionPendiente", OpinionPendienteResponse.class),
                arguments("OpinionEnModeracion", OpinionEnModeracionResponse.class),
                arguments("AvisoResultado", AvisoOpinionResponse.class),
                arguments("PerfilRequest", PerfilRequest.class),
                arguments("Foto", FotoResponse.class));
    }

    private Set<String> rutasDeLosControladores() {
        Set<String> resultado = new TreeSet<>();
        rutas.getHandlerMethods().forEach((RequestMappingInfo ruta, org.springframework.web.method.HandlerMethod metodo) -> {
            if (PAQUETES_CONTROLADORES.contains(metodo.getBeanType().getPackageName())) {
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
