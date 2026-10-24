package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion.EncolarAvisoService;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion.PublicarAvisosService;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.EncolarAvisoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.PublicarAvisosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.CatalogoAvisosPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/** Crea los casos de uso de la bandeja de salida y el cliente HTTP hacia el catálogo (DN-06). */
@Configuration
public class BandejaConfig {

    /** RNF-05: una fuente externa que no responde en 3 s cuenta como caída. */
    static final Duration ESPERA_CATALOGO = Duration.ofSeconds(3);

    @Bean
    EncolarAvisoUseCase encolarAviso(BandejaSalidaRepositoryPort bandeja, Clock reloj) {
        return new EncolarAvisoService(bandeja, reloj);
    }

    @Bean
    PublicarAvisosUseCase publicarAvisos(BandejaSalidaRepositoryPort bandeja, CatalogoAvisosPort catalogo,
                                         Clock reloj) {
        return new PublicarAvisosService(bandeja, catalogo, reloj);
    }

    /** @param url la base de la API del catálogo, con su prefijo; en Docker Compose, por la red interna */
    @Bean
    RestClient catalogoAvisos(@Value("${nucleo.catalogo.url}") String url) {
        return clienteCatalogo(url);
    }

    /** El cliente hacia el catálogo, con la espera de 3 s para conectar y para recibir la respuesta. */
    public static RestClient clienteCatalogo(String url) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA_CATALOGO).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(ESPERA_CATALOGO);
        return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }
}
