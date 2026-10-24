package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.compartido.http.ClientesHttp;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo.CatalogoRestClientAdapter;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo.PropiedadesCatalogo;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cliente del catálogo, con su corte por petición y su caché (DR-07).
 */
@Configuration
@EnableConfigurationProperties(PropiedadesCatalogo.class)
public class CatalogoClienteConfig {

    @Bean
    CatalogoConsultaPort catalogoConsulta(RestClient.Builder clientes, PropiedadesCatalogo propiedades) {
        RestClient catalogo = clientes.baseUrl(propiedades.url())
                .requestFactory(ClientesHttp.conTiempoMaximo(propiedades.timeout()))
                .build();
        return new CatalogoRestClientAdapter(catalogo, propiedades.vigenciaCache());
    }
}
