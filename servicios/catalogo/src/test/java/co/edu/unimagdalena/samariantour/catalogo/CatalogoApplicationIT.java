package co.edu.unimagdalena.samariantour.catalogo;

import co.edu.unimagdalena.samariantour.seguridadjwt.ClavesRsa;
import co.edu.unimagdalena.samariantour.seguridadjwt.EmisorTokenServicio;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El servicio completo con el perfil {@code mongo}: los casos de uso conectados a los adaptadores de MongoDB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mongo")
class CatalogoApplicationIT extends IntegracionMongoBase {

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private TestRestTemplate http;

    @Test
    void arrancaConectadoAMongo() {
        Document respuesta = mongo.getDb().runCommand(new Document("ping", 1));

        assertThat(respuesta.get("ok", Number.class).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("con el perfil mongo, la salud incluye MongoDB y la API lee de la base")
    void apiConMongo() {
        var salud = http.getForEntity("/actuator/health", String.class);
        assertThat(salud.getStatusCode()).isEqualTo(HttpStatus.OK);

        var busqueda = http.getForEntity("/ofertas?tipo=ACTIVIDAD", String.class);
        assertThat(busqueda.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(busqueda.getBody()).contains("\"contenido\"");
    }

    @Test
    @DisplayName("con el perfil mongo responden las consultas internas, cada una con el token del servicio que la usa")
    void consultasInternasConMongo() {
        var visibles = http.exchange("/internal/ofertas?tipo=PAQUETE", HttpMethod.GET,
                con(firmadoPor(ServiciosInternos.RECOMENDACION, TokensDePrueba.EMISOR_RECOMENDACION,
                        TokensDePrueba.CLAVE_PRIVADA_RECOMENDACION)), String.class);
        assertThat(visibles.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(visibles.getBody()).contains("\"contenido\"");

        var inexistente = http.exchange("/internal/ofertas/b00000000000000000000099", HttpMethod.GET,
                con(firmadoPor(ServiciosInternos.NUCLEO, TokensDePrueba.EMISOR, TokensDePrueba.CLAVE_PRIVADA)),
                String.class);
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(inexistente.getBody()).contains("OFERTA_NO_ENCONTRADA");

        // Sin el token de servicio no se llega, aunque la petición venga de la red de Docker (DN-38).
        assertThat(http.getForEntity("/internal/ofertas?tipo=PAQUETE", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** El token como lo firma el servicio de verdad, con {@link EmisorTokenServicio}. */
    private static String firmadoPor(String servicio, String emisor, String clavePrivada) {
        return new EmisorTokenServicio(servicio, emisor, ClavesRsa.privada(clavePrivada), Clock.systemUTC())
                .token(ServiciosInternos.CATALOGO);
    }

    private static HttpEntity<Void> con(String token) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setBearerAuth(token);
        return new HttpEntity<>(cabeceras);
    }

    @Test
    void respondeLosErroresComoProblemDetailConElHeaderDeInstancia() {
        var respuesta = http.getForEntity("/ruta-inexistente", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(respuesta.getHeaders().getFirst("X-Instancia")).isNotBlank();
    }
}
