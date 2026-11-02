package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.DistribucionCategoria;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.EmbudoConversion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.EtapaEmbudo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Tablero;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarDistribucionCategoriaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarEmbudoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarTableroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.DemandaZonaResponse;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.DistribucionCategoriaResponse;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.EmbudoResponse;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.EmbudoTipoResponse;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.EtapaEmbudoResponse;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.ParticipacionCategoriaResponse;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.TableroResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Los indicadores del gestor del destino (RF-85 a RF-89), con el rol GESTOR_DESTINO (DN-05). Solo datos agregados:
 * un conteo de 1 o 2 no sale en la respuesta (RET-03, DN-41).
 */
@RestController
@RequestMapping("/gestor/indicadores")
public class IndicadoresController {

    private final ConsultarTableroUseCase tablero;
    private final ConsultarEmbudoUseCase embudo;
    private final ConsultarDistribucionCategoriaUseCase distribucion;

    public IndicadoresController(ConsultarTableroUseCase tablero, ConsultarEmbudoUseCase embudo,
                                 ConsultarDistribucionCategoriaUseCase distribucion) {
        this.tablero = tablero;
        this.embudo = embudo;
        this.distribucion = distribucion;
    }

    @GetMapping("/tablero")
    public TableroResponse tablero(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Tablero resultado = tablero.consultarTablero(desde, hasta);
        return new TableroResponse(resultado.periodo().desde(), resultado.periodo().hasta(), resultado.total(),
                resultado.filas().stream()
                        .map(fila -> new DemandaZonaResponse(fila.zonaId(), fila.tipoOferta(), fila.solicitudes()))
                        .toList());
    }

    @GetMapping("/embudo")
    public EmbudoResponse embudo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        EmbudoConversion resultado = embudo.consultarEmbudo(desde, hasta);
        return new EmbudoResponse(resultado.periodo().desde(), resultado.periodo().hasta(),
                etapas(resultado.etapas()), resultado.porTipo().stream()
                        .map(tipo -> new EmbudoTipoResponse(tipo.tipoOferta(), etapas(tipo.etapas())))
                        .toList());
    }

    @GetMapping("/categorias")
    public DistribucionCategoriaResponse categorias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        DistribucionCategoria resultado = distribucion.consultarDistribucion(desde, hasta);
        return new DistribucionCategoriaResponse(resultado.periodo().desde(), resultado.periodo().hasta(),
                resultado.total(), resultado.categorias().stream()
                        .map(categoria -> new ParticipacionCategoriaResponse(categoria.categoria(),
                                categoria.solicitudes(), categoria.porcentaje(), categoria.pagadas()))
                        .toList());
    }

    private static List<EtapaEmbudoResponse> etapas(List<EtapaEmbudo> etapas) {
        return etapas.stream()
                .map(etapa -> new EtapaEmbudoResponse(etapa.etapa().name(), etapa.solicitudes(), etapa.porcentaje()))
                .toList();
    }
}
