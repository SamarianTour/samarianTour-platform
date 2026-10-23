package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.config;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.AplicarAvisoNucleoService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.BuscarOfertasService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarFichaOfertaService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarOfertaInternaService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarOfertasVisiblesService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarZonasService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.GestionarOfertaService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ListarPendientesModeracionService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.AplicarAvisoNucleoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.BuscarOfertasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFichaOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaInternaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertasVisiblesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ListarPendientesModeracionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.AvisoProcesadoPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Crea los servicios de aplicación del módulo catálogo y les entrega los adaptadores de salida activos.
 * Los servicios no llevan anotaciones de Spring: aquí es donde se conectan los puertos con sus adaptadores.
 */
@Configuration
public class CatalogoConfig {

    @Bean
    BuscarOfertasUseCase buscarOfertas(OfertaRepositoryPort ofertas) {
        return new BuscarOfertasService(ofertas);
    }

    @Bean
    ConsultarFichaOfertaUseCase consultarFichaOferta(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas,
                                                     PerfilRepositoryPort perfiles) {
        return new ConsultarFichaOfertaService(ofertas, zonas, perfiles);
    }

    @Bean
    ConsultarOfertaInternaUseCase consultarOfertaInterna(OfertaRepositoryPort ofertas) {
        return new ConsultarOfertaInternaService(ofertas);
    }

    @Bean
    ConsultarOfertasVisiblesUseCase consultarOfertasVisibles(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas) {
        return new ConsultarOfertasVisiblesService(ofertas, zonas);
    }

    @Bean
    ListarPendientesModeracionUseCase listarPendientesModeracion(OfertaRepositoryPort ofertas) {
        return new ListarPendientesModeracionService(ofertas);
    }

    @Bean
    ConsultarZonasUseCase consultarZonas(ZonaRepositoryPort zonas) {
        return new ConsultarZonasService(zonas);
    }

    @Bean
    GestionarOfertaUseCase gestionarOferta(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas,
                                           PrestadorCatalogoRepositoryPort prestadores, Clock reloj,
                                           EjecutorConReintento reintento) {
        return new GestionarOfertaService(ofertas, zonas, prestadores, reloj, reintento);
    }

    @Bean
    AplicarAvisoNucleoUseCase aplicarAvisoNucleo(OfertaRepositoryPort ofertas,
                                                 PrestadorCatalogoRepositoryPort prestadores,
                                                 AvisoProcesadoPort avisosProcesados, Clock reloj,
                                                 EjecutorConReintento reintento) {
        return new AplicarAvisoNucleoService(ofertas, prestadores, avisosProcesados, reloj, reintento);
    }
}
