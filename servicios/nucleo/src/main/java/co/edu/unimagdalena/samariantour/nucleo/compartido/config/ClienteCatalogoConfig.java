package co.edu.unimagdalena.samariantour.nucleo.compartido.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * El cliente HTTP hacia la API interna del catálogo, que usan los módulos del núcleo: la bandeja de salida para enviar
 * los avisos (DN-06) y la administración para consultar lo que va a moderar (DN-08).
 */
@Configuration
public class ClienteCatalogoConfig {

    /** RNF-05: una fuente externa que no responde en 3 s cuenta como caída. */
    public static final Duration ESPERA_CATALOGO = Duration.ofSeconds(3);

    /** @param url la base de la API del catálogo, con su prefijo; en Docker Compose, por la red interna */
    @Bean
    RestClient clienteCatalogo(@Value("${nucleo.catalogo.url}") String url) {
        return crear(url);
    }

    /** El cliente hacia el catálogo, con la espera de 3 s para conectar y para recibir la respuesta. */
    public static RestClient crear(String url) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA_CATALOGO).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(ESPERA_CATALOGO);
        return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }
}
