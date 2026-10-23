package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.BuscarOfertasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFichaOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FichaOfertaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaResumenResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.BusquedaWebMapper;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.OfertaWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Catálogo público: búsqueda de ofertas visibles y ficha de una oferta (RF-14 a RF-17, RF-20). No exige identidad.
 */
@RestController
@RequestMapping("/ofertas")
public class OfertaPublicaController {

    private final BuscarOfertasUseCase buscarOfertas;
    private final ConsultarFichaOfertaUseCase consultarFicha;

    public OfertaPublicaController(BuscarOfertasUseCase buscarOfertas, ConsultarFichaOfertaUseCase consultarFicha) {
        this.buscarOfertas = buscarOfertas;
        this.consultarFicha = consultarFicha;
    }

    @GetMapping
    public PaginaResponse<OfertaResumenResponse> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String zonaId,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) String tipoActividad,
            @RequestParam(required = false) String nivelEsfuerzoMaximo,
            @RequestParam(required = false) Boolean accesibleSillaRuedas,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return OfertaWebMapper.aPagina(buscarOfertas.buscar(BusquedaWebMapper.aCriterios(texto, tipo, zonaId,
                precioMin, precioMax, tipoActividad, nivelEsfuerzoMaximo, accesibleSillaRuedas, page, size)));
    }

    @GetMapping("/{id}")
    public FichaOfertaResponse ficha(@PathVariable String id) {
        return OfertaWebMapper.aFicha(consultarFicha.consultar(id));
    }
}
