package co.edu.unimagdalena.samariantour.catalogo;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    void respondeLosErroresComoProblemDetailConElHeaderDeInstancia() {
        var respuesta = http.getForEntity("/ruta-inexistente", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(respuesta.getHeaders().getFirst("X-Instancia")).isNotBlank();
    }
}
