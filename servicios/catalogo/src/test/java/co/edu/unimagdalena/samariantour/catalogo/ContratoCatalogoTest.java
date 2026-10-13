package co.edu.unimagdalena.samariantour.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API first: las rutas de los controladores del catálogo son exactamente las del contrato
 * {@code contratos/catalogo.openapi.yaml}. Si una ruta existe en un lado y no en el otro, esta prueba falla.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration")
@ActiveProfiles({"memoria", "demo"})
class ContratoCatalogoTest {

    /** La prueba corre con el módulo como directorio de trabajo; el contrato está en la raíz del repositorio. */
    private static final Path CONTRATO = Path.of("..", "..", "contratos", "catalogo.openapi.yaml");
    private static final Set<String> METODOS = Set.of("get", "post", "put", "patch", "delete");
    private static final String PAQUETE_CONTROLADORES =
            "co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rutas;

    @Test
    @DisplayName("los controladores implementan exactamente las rutas del contrato OpenAPI")
    void rutasDelContrato() throws IOException {
        assertThat(rutasDeLosControladores()).isEqualTo(rutasDelContratoOpenApi());
    }

    private Set<String> rutasDeLosControladores() {
        Set<String> resultado = new TreeSet<>();
        rutas.getHandlerMethods().forEach((RequestMappingInfo ruta, org.springframework.web.method.HandlerMethod metodo) -> {
            if (metodo.getBeanType().getPackageName().equals(PAQUETE_CONTROLADORES)) {
                ruta.getMethodsCondition().getMethods().forEach(verbo -> ruta.getPatternValues()
                        .forEach(patron -> resultado.add(verbo + " " + patron)));
            }
        });
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> rutasDelContratoOpenApi() throws IOException {
        Map<String, Object> contrato;
        try (Reader lector = Files.newBufferedReader(CONTRATO, StandardCharsets.UTF_8)) {
            contrato = new Yaml().load(lector);
        }
        Set<String> resultado = new TreeSet<>();
        ((Map<String, Map<String, Object>>) contrato.get("paths")).forEach((ruta, operaciones) ->
                operaciones.keySet().stream().filter(METODOS::contains)
                        .forEach(verbo -> resultado.add(verbo.toUpperCase() + " " + ruta)));
        return resultado;
    }
}
