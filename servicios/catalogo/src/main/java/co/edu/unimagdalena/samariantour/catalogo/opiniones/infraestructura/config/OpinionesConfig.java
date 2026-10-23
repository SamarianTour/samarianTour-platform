package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.config;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion.LeerOpinionesOfertaService;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion.ListarOpinionesTuristaService;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion.PublicarOpinionService;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.LeerOpinionesOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesTuristaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.PublicarOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Casos de uso del turista: publicar una opinión, leer las de una oferta y ver las propias. Los del administrador y
 * del núcleo están en {@code ModeracionOpinionesConfig}.
 */
@Configuration
public class OpinionesConfig {

    @Bean
    PublicarOpinionUseCase publicarOpinion(OpinionRepositoryPort opiniones,
                                           HabilitacionOpinionRepositoryPort habilitaciones,
                                           OfertaCatalogoPort ofertas, Clock reloj) {
        return new PublicarOpinionService(opiniones, habilitaciones, ofertas, reloj);
    }

    @Bean
    LeerOpinionesOfertaUseCase leerOpinionesOferta(OpinionRepositoryPort opiniones, OfertaCatalogoPort ofertas) {
        return new LeerOpinionesOfertaService(opiniones, ofertas);
    }

    @Bean
    ListarOpinionesTuristaUseCase listarOpinionesTurista(OpinionRepositoryPort opiniones, OfertaCatalogoPort ofertas) {
        return new ListarOpinionesTuristaService(opiniones, ofertas);
    }
}
