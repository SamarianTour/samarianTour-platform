package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.compartido.http.ClientesHttp;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.ExposicionPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion.ExposicionLogAdapter;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion.ExposicionRestAdapter;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion.PropiedadesNucleo;
import co.edu.unimagdalena.samariantour.seguridadjwt.EmisorTokenServicio;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * El registro de exposición va al núcleo con el token de servicio de la recomendación (DN-38, DR-24); el log queda de
 * respaldo (DR-08).
 */
@Configuration
@EnableConfigurationProperties(PropiedadesNucleo.class)
public class ExposicionConfig {

    @Bean
    ExposicionPort exposicion(RestClient.Builder clientes, PropiedadesNucleo propiedades, EmisorTokenServicio tokens,
                              ObjectMapper json) {
        RestClient nucleo = clientes.baseUrl(propiedades.url())
                .requestFactory(ClientesHttp.conTiempoMaximo(propiedades.timeout()))
                .requestInterceptor(tokens.interceptor(ServiciosInternos.NUCLEO))
                .build();
        return new ExposicionRestAdapter(nucleo, new ExposicionLogAdapter(json));
    }
}
