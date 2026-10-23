package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.Identidad;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesTuristaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.PublicarOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.MiOpinionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionRequest;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.mapper.OpinionWebMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Opiniones del turista: dejar una opinión (RF-60) y ver las propias. Las rutas van bajo {@code /turista}, así que
 * {@code ExigirRol} exige el rol TURISTA antes de llegar aquí (DC-13); el turista es el de la identidad verificada.
 */
@RestController
@RequestMapping("/turista/opiniones")
public class OpinionTuristaController {

    private final PublicarOpinionUseCase publicarOpinion;
    private final ListarOpinionesTuristaUseCase listarOpiniones;

    public OpinionTuristaController(PublicarOpinionUseCase publicarOpinion,
                                    ListarOpinionesTuristaUseCase listarOpiniones) {
        this.publicarOpinion = publicarOpinion;
        this.listarOpiniones = listarOpiniones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MiOpinionResponse publicar(@RequestBody OpinionRequest opinion, HttpServletRequest peticion) {
        return OpinionWebMapper.aMiOpinion(publicarOpinion.publicar(
                OpinionWebMapper.aComando(Identidad.de(peticion).usuarioId(), opinion)));
    }

    @GetMapping
    public PaginaResponse<MiOpinionResponse> misOpiniones(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size,
                                                          HttpServletRequest peticion) {
        return OpinionWebMapper.aPaginaMisOpiniones(
                listarOpiniones.listar(Identidad.de(peticion).usuarioId(), Paginacion.de(page, size)));
    }
}
