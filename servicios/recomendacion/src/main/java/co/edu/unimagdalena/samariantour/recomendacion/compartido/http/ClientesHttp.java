package co.edu.unimagdalena.samariantour.recomendacion.compartido.http;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Fábrica de clientes HTTP con tiempo máximo de conexión y de respuesta. Sin él, un servicio que no responde dejaría
 * la consulta del turista esperando indefinidamente (QAS-03, ADR-04).
 */
public final class ClientesHttp {

    private ClientesHttp() {
    }

    public static ClientHttpRequestFactory conTiempoMaximo(Duration tiempoMaximo) {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(tiempoMaximo)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(tiempoMaximo);
        return fabrica;
    }
}
