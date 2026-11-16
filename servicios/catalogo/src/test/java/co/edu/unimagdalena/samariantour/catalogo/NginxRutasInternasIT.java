package co.edu.unimagdalena.samariantour.catalogo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La configuración real de Nginx ({@code infra/nginx/nginx.conf}, con las rutas de {@code rutas.conf}) frente a las
 * rutas que intentan llegar desde afuera a {@code /internal/**} o {@code /actuator} de los tres servicios (DN-37).
 * Detrás de Nginx hay un eco que responde la ruta que le llegó con los nombres de los tres servicios: si una petición
 * bloqueada lo alcanzara, la respuesta lo diría. También las cabeceras de seguridad del navegador y el límite de
 * peticiones de las rutas de cuentas. La configuración de AWS, que incluye las mismas rutas, la prueba
 * {@link NginxAwsIT}.
 */
class NginxRutasInternasIT {

    // Como Spring Security en los servicios, el eco manda X-Frame-Options y X-Content-Type-Options: Nginx debe
    // quitarlas para que no lleguen repetidas.
    private static final String ECO = """
            events {}
            http {
                server {
                    listen 8080;
                    location / {
                        default_type text/plain;
                        add_header X-Frame-Options "DENY";
                        add_header X-Content-Type-Options "nosniff";
                        return 200 "eco $request_uri";
                    }
                }
            }
            """;

    private static final Network RED = Network.newNetwork();

    private static final GenericContainer<?> SERVICIOS = new GenericContainer<>("nginx:1.27-alpine")
            .withNetwork(RED)
            .withNetworkAliases("catalogo", "nucleo", "recomendacion")
            .withCopyToContainer(Transferable.of(ECO), "/etc/nginx/nginx.conf");

    private static final GenericContainer<?> NGINX = new GenericContainer<>("nginx:1.27-alpine")
            .withNetwork(RED)
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../../infra/nginx/nginx.conf")),
                    "/etc/nginx/nginx.conf")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../../infra/nginx/rutas.conf")),
                    "/etc/nginx/rutas.conf")
            .withExposedPorts(8080)
            .dependsOn(SERVICIOS)
            .waitingFor(Wait.forHttp("/").forPort(8080));

    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @BeforeAll
    static void iniciar() {
        NGINX.start();
    }

    @AfterAll
    static void detener() {
        NGINX.stop();
        SERVICIOS.stop();
        RED.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/catalogo/internal/avisos",
            "/api/catalogo/internal/ofertas/b00000000000000000000014",
            "/api/catalogo/%69nternal/avisos",
            "/api/catalogo//internal/avisos",
            "/api/catalogo/x/../internal/avisos",
            "/api/catalogo/actuator",
            "/api/catalogo/actuator/health",
            "/api/nucleo/internal/algo",
            "/api/nucleo/actuator",
            "/api/nucleo/actuator/health",
            "/api/recomendacion/internal/algo",
            "/api/recomendacion/actuator/health"})
    @DisplayName("las rutas internas y la salud, aun codificadas o con // y .., responden 404 sin llegar al servicio")
    void bloqueadas(String ruta) throws Exception {
        HttpResponse<String> respuesta = pedir(ruta);

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(respuesta.body()).doesNotContain("eco");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/catalogo/internal;x/avisos",
            "/api/catalogo/internal;/ofertas/b00000000000000000000014",
            "/api/catalogo/internal%3Bx/avisos",
            "/api/nucleo/actuator;x/health",
            "/api/recomendacion/actuator;/health",
            "/api/catalogo/ofertas;jsessionid=1"})
    @DisplayName("una ruta con ; o %3B responde 400 sin llegar al servicio: era la forma de esquivar el bloqueo")
    void conPuntoYComa(String ruta) throws Exception {
        HttpResponse<String> respuesta = pedir(ruta);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.body()).doesNotContain("eco");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/catalogo/ofertas?texto=playa;tayrona",
            "/api/catalogo/zonas",
            "/api/nucleo/cuentas/sesion",
            "/api/recomendacion/alertas",
            "/api/catalogo/internalizar"})
    @DisplayName("las rutas públicas llegan al servicio tal como se pidieron, con ; en la consulta")
    void publicas(String ruta) throws Exception {
        HttpResponse<String> respuesta = pedir(ruta);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo("eco " + ruta);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/ofertas/b00000000000000000000001", "/api/catalogo/zonas",
            "/api/nucleo/internal/algo", "/api/catalogo/internal;x/avisos"})
    @DisplayName("el frontend, la API y los errores de Nginx llevan las cabeceras de seguridad, cada una una sola vez")
    void cabecerasDeSeguridad(String ruta) throws Exception {
        HttpHeaders cabeceras = pedir(ruta).headers();

        assertThat(cabeceras.allValues("Content-Security-Policy")).singleElement().asString()
                .contains("default-src 'self'", "script-src 'self';", "object-src 'none'", "frame-ancestors 'none'")
                .doesNotContain("unsafe-eval")
                .satisfies(csp -> assertThat(csp.split("script-src")[1].split(";")[0]).doesNotContain("unsafe-inline"));
        assertThat(cabeceras.allValues("X-Frame-Options")).containsExactly("DENY");
        assertThat(cabeceras.allValues("X-Content-Type-Options")).containsExactly("nosniff");
        assertThat(cabeceras.allValues("Referrer-Policy")).containsExactly("strict-origin-when-cross-origin");
        assertThat(cabeceras.allValues("Permissions-Policy")).containsExactly("camera=(), microphone=(), geolocation=()");
    }

    @Test
    @DisplayName("las rutas de cuentas sin sesión admiten 21 peticiones seguidas por IP; después, 429 sin llegar al núcleo")
    void limiteDePeticiones() throws Exception {
        List<HttpResponse<String>> respuestas = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            respuestas.add(pedir("/api/nucleo/cuentas/registro"));
        }
        HttpResponse<String> ultima = respuestas.getLast();
        HttpResponse<String> otraRuta = pedir("/api/nucleo/cuentas/sesion/renovacion");

        // Una más la ráfaga de 20. Si la prueba tardara más de 6 s, se repondría una: por eso se miran las 21 primeras.
        assertThat(respuestas.subList(0, 21)).allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(200));
        assertThat(ultima.statusCode()).isEqualTo(429);
        assertThat(ultima.headers().firstValue("Content-Type")).contains("application/problem+json");
        assertThat(ultima.body()).contains("\"status\":429").doesNotContain("eco");
        assertThat(ultima.headers().firstValue("Content-Security-Policy")).isPresent();
        // El límite es por ruta: la renovación de la sesión no comparte el cupo del registro.
        assertThat(otraRuta.statusCode()).isEqualTo(200);
    }

    private static HttpResponse<String> pedir(String ruta) throws Exception {
        URI uri = URI.create("http://" + NGINX.getHost() + ":" + NGINX.getMappedPort(8080) + ruta);
        return CLIENTE.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
