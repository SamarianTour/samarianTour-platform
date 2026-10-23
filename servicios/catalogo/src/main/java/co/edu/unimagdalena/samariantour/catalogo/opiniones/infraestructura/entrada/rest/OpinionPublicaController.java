package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.LeerOpinionesOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionesOfertaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.mapper.OpinionWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lectura pública de las opiniones de una oferta (RF-59, DC-43). Va aparte de la ficha: el catálogo no depende de
 * opiniones, así que el frontend pide las opiniones a esta ruta (DC-40).
 */
@RestController
public class OpinionPublicaController {

    private final LeerOpinionesOfertaUseCase leerOpiniones;

    public OpinionPublicaController(LeerOpinionesOfertaUseCase leerOpiniones) {
        this.leerOpiniones = leerOpiniones;
    }

    @GetMapping("/ofertas/{id}/opiniones")
    public OpinionesOfertaResponse opiniones(@PathVariable String id,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return OpinionWebMapper.aOpinionesOferta(leerOpiniones.leer(id, Paginacion.de(page, size)));
    }
}
