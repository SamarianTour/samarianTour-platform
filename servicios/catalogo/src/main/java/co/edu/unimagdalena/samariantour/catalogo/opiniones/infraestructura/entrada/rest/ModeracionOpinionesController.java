package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesPendientesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionEnModeracionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionPendienteResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.mapper.ModeracionOpinionesWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que espera la decisión del administrador (RF-80, DC-44), con la misma forma que las ofertas en moderación
 * (DC-38). El administrador lo lee aquí, con el rol ADMINISTRADOR (DC-13), y decide en el núcleo; el núcleo consulta
 * la opinión por la ruta interna y envía la decisión como aviso.
 */
@RestController
public class ModeracionOpinionesController {

    private final ListarOpinionesPendientesUseCase pendientes;

    public ModeracionOpinionesController(ListarOpinionesPendientesUseCase pendientes) {
        this.pendientes = pendientes;
    }

    @GetMapping("/admin/moderacion/opiniones")
    public PaginaResponse<OpinionPendienteResponse> opinionesPendientes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ModeracionOpinionesWebMapper.aPaginaPendientes(pendientes.listar(Paginacion.de(page, size)));
    }

    /** Nginx no expone {@code /internal/**} hacia afuera, y solo el núcleo entra, con su token de servicio (DN-38). */
    @GetMapping("/internal/moderacion/opiniones/{id}")
    public OpinionEnModeracionResponse opinionEnModeracion(@PathVariable String id) {
        return ModeracionOpinionesWebMapper.aEnModeracion(pendientes.consultar(id));
    }
}
