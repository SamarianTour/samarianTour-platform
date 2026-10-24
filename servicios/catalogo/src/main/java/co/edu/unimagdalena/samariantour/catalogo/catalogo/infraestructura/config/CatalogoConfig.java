package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.config;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.AplicarAvisoNucleoService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.BuscarOfertasService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarFichaOfertaService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarFotoService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarOfertaInternaService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarOfertaParaModulosService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarOfertasVisiblesService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ConsultarZonasService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.GestionarZonasService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.GestionarOfertaService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.GestionarPerfilPublicoService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.LimpiarFotosSinUsoService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.ListarPendientesModeracionService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.SubirFotoService;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.AplicarAvisoNucleoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.BuscarOfertasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFichaOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFotoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaInternaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaParaModulosUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertasVisiblesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarPerfilPublicoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.LimpiarFotosSinUsoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ListarPendientesModeracionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.SubirFotoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.AvisoProcesadoPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
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
    ConsultarOfertaParaModulosUseCase consultarOfertaParaModulos(OfertaRepositoryPort ofertas) {
        return new ConsultarOfertaParaModulosService(ofertas);
    }

    @Bean
    ConsultarOfertasVisiblesUseCase consultarOfertasVisibles(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas) {
        return new ConsultarOfertasVisiblesService(ofertas, zonas);
    }

    @Bean
    GestionarPerfilPublicoUseCase gestionarPerfilPublico(PerfilRepositoryPort perfiles,
                                                         FotoSubidaRepositoryPort registroFotos,
                                                         FileStoragePort fotos, Clock reloj,
                                                         EjecutorConReintento reintento) {
        return new GestionarPerfilPublicoService(perfiles, registroFotos, fotos, reloj, reintento);
    }

    @Bean
    SubirFotoUseCase subirFoto(FotoSubidaRepositoryPort registroFotos, FileStoragePort fotos, Clock reloj) {
        return new SubirFotoService(registroFotos, fotos, reloj);
    }

    @Bean
    LimpiarFotosSinUsoUseCase limpiarFotosSinUso(FotoSubidaRepositoryPort registroFotos, FileStoragePort fotos,
                                                 OfertaRepositoryPort ofertas, PerfilRepositoryPort perfiles,
                                                 Clock reloj) {
        return new LimpiarFotosSinUsoService(registroFotos, fotos, ofertas, perfiles, reloj);
    }

    @Bean
    ConsultarFotoUseCase consultarFoto(FileStoragePort fotos) {
        return new ConsultarFotoService(fotos);
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
    GestionarZonasUseCase gestionarZonas(ZonaRepositoryPort zonas) {
        return new GestionarZonasService(zonas);
    }

    @Bean
    GestionarOfertaUseCase gestionarOferta(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas,
                                           PrestadorCatalogoRepositoryPort prestadores,
                                           FotoSubidaRepositoryPort registroFotos, Clock reloj,
                                           EjecutorConReintento reintento) {
        return new GestionarOfertaService(ofertas, zonas, prestadores, registroFotos, reloj, reintento);
    }

    @Bean
    AplicarAvisoNucleoUseCase aplicarAvisoNucleo(OfertaRepositoryPort ofertas,
                                                 PrestadorCatalogoRepositoryPort prestadores,
                                                 AvisoProcesadoPort avisosProcesados, Clock reloj,
                                                 EjecutorConReintento reintento) {
        return new AplicarAvisoNucleoService(ofertas, prestadores, avisosProcesados, reloj, reintento);
    }
}
