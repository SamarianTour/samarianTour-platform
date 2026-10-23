package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ListarPendientesModeracionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaEnModeracionResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaPendienteResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.ModeracionWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que espera la decisión del administrador (RF-79, DC-38). El administrador lo lee aquí, con el rol
 * ADMINISTRADOR (DC-13), y decide en el núcleo; el núcleo consulta la oferta por la ruta interna y envía la decisión
 * como aviso de moderación (DC-04).
 */
@RestController
public class ModeracionController {

    private final ListarPendientesModeracionUseCase pendientes;

    public ModeracionController(ListarPendientesModeracionUseCase pendientes) {
        this.pendientes = pendientes;
    }

    @GetMapping("/admin/moderacion/ofertas")
    public PaginaResponse<OfertaPendienteResponse> ofertasPendientes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ModeracionWebMapper.aPaginaPendientes(pendientes.listarOfertas(Paginacion.de(page, size)));
    }

    /** Nginx no expone {@code /internal/**} hacia afuera; la autenticación entre servicios llega con el JWT. */
    @GetMapping("/internal/moderacion/ofertas/{id}")
    public OfertaEnModeracionResponse ofertaEnModeracion(@PathVariable String id) {
        return ModeracionWebMapper.aEnModeracion(pendientes.consultarOferta(id));
    }
}
