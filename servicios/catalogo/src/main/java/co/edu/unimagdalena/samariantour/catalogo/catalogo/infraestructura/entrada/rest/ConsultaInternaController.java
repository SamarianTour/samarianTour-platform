package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaInternaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertasVisiblesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ContarOfertasVisiblesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaInternaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaVisibleResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertasPrestadorResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.ConsultaInternaWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consultas de los otros servicios: el núcleo pide una oferta al crear una solicitud (RF-30) y cuántas visibles tiene
 * cada prestador (RET-02, DC-51); la recomendación lee las ofertas visibles (RF-26, RF-29). Nginx no expone
 * {@code /internal/**} hacia afuera, y cada consulta exige el token de servicio de quien la usa: la lista, la
 * recomendación; una oferta y el conteo, el núcleo (DC-04, DN-38).
 */
@RestController
@RequestMapping("/internal/ofertas")
public class ConsultaInternaController {

    private final ConsultarOfertaInternaUseCase consultarOferta;
    private final ConsultarOfertasVisiblesUseCase consultarVisibles;
    private final ContarOfertasVisiblesUseCase contarVisibles;

    public ConsultaInternaController(ConsultarOfertaInternaUseCase consultarOferta,
                                     ConsultarOfertasVisiblesUseCase consultarVisibles,
                                     ContarOfertasVisiblesUseCase contarVisibles) {
        this.consultarOferta = consultarOferta;
        this.consultarVisibles = consultarVisibles;
        this.contarVisibles = contarVisibles;
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

    @GetMapping("/por-prestador")
    public List<OfertasPrestadorResponse> porPrestador() {
        return contarVisibles.porPrestador().stream()
                .map(conteo -> new OfertasPrestadorResponse(conteo.prestadorId(), conteo.ofertas()))
                .toList();
    }

    @GetMapping("/{id}")
    public OfertaInternaResponse oferta(@PathVariable String id) {
        return ConsultaInternaWebMapper.aOfertaInterna(consultarOferta.consultar(id));
    }
}
