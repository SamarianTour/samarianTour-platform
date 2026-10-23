package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaInternaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertasVisiblesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaInternaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaVisibleResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.ConsultaInternaWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas de los otros servicios: el núcleo pide una oferta al crear una solicitud (RF-30) y la recomendación lee
 * las ofertas visibles (RF-26, RF-29, RET-02). Nginx no expone {@code /internal/**} hacia afuera; la autenticación
 * entre servicios queda pendiente del JWT (DC-04).
 */
@RestController
@RequestMapping("/internal/ofertas")
public class ConsultaInternaController {

    private final ConsultarOfertaInternaUseCase consultarOferta;
    private final ConsultarOfertasVisiblesUseCase consultarVisibles;

    public ConsultaInternaController(ConsultarOfertaInternaUseCase consultarOferta,
                                     ConsultarOfertasVisiblesUseCase consultarVisibles) {
        this.consultarOferta = consultarOferta;
        this.consultarVisibles = consultarVisibles;
    }

    @GetMapping
    public PaginaResponse<OfertaVisibleResponse> visibles(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String zonaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ConsultaInternaWebMapper.aPaginaVisibles(consultarVisibles.consultar(
                ConsultaInternaWebMapper.aTipo(tipo), zonaId, Paginacion.de(page, size)));
    }

    @GetMapping("/{id}")
    public OfertaInternaResponse oferta(@PathVariable String id) {
        return ConsultaInternaWebMapper.aOfertaInterna(consultarOferta.consultar(id));
    }
}
