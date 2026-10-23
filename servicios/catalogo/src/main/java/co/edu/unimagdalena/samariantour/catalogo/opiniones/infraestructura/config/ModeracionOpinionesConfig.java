package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.config;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion.AplicarAvisoOpinionService;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion.ListarOpinionesPendientesService;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.AplicarAvisoOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesPendientesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.AvisoOpinionProcesadoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Casos de uso del administrador y del núcleo: la cola de moderación y los avisos (habilitación, moderación y
 * supresión). Los del turista están en {@link OpinionesConfig}.
 */
@Configuration
public class ModeracionOpinionesConfig {

    @Bean
    ListarOpinionesPendientesUseCase listarOpinionesPendientes(OpinionRepositoryPort opiniones,
                                                               OfertaCatalogoPort ofertas) {
        return new ListarOpinionesPendientesService(opiniones, ofertas);
    }

    @Bean
    AplicarAvisoOpinionUseCase aplicarAvisoOpinion(OpinionRepositoryPort opiniones,
                                                   HabilitacionOpinionRepositoryPort habilitaciones,
                                                   OfertaCatalogoPort ofertas,
                                                   AvisoOpinionProcesadoPort avisosProcesados, Clock reloj,
                                                   EjecutorConReintento reintento) {
        return new AplicarAvisoOpinionService(opiniones, habilitaciones, ofertas, avisosProcesados, reloj, reintento);
    }
}
