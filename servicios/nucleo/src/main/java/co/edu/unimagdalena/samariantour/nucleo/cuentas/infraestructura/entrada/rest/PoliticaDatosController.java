package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarPoliticaDatosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.PoliticaDatosResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * La política de tratamiento de datos, abierta a cualquiera: quien se registra la lee antes de aceptarla (RF-09 a
 * RF-11). El texto de cada versión está en {@code src/main/resources/politica-datos/<version>.md}.
 */
@RestController
public class PoliticaDatosController {

    private static final MediaType MARKDOWN = new MediaType("text", "markdown", StandardCharsets.UTF_8);
    /** Solo letras, números y puntos: el nombre de la versión no puede salir de la carpeta. */
    private static final Pattern VERSION = Pattern.compile("^[a-z0-9.]{1,20}$");

    private final ConsultarPoliticaDatosUseCase politicas;

    public PoliticaDatosController(ConsultarPoliticaDatosUseCase politicas) {
        this.politicas = politicas;
    }

    @GetMapping("/politica-datos/vigente")
    public PoliticaDatosResponse vigente() {
        PoliticaDatos vigente = politicas.vigente();
        return new PoliticaDatosResponse(vigente.version(), vigente.fechaPublicacion(), vigente.urlTexto());
    }

    @GetMapping("/politica-datos/{version}")
    public ResponseEntity<String> texto(@PathVariable String version) throws IOException {
        ClassPathResource archivo = new ClassPathResource("politica-datos/" + version + ".md");
        if (!VERSION.matcher(version).matches() || !archivo.exists()) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream entrada = archivo.getInputStream()) {
            return ResponseEntity.ok().contentType(MARKDOWN)
                    .body(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
