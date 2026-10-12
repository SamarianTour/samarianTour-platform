package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.PublicarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.Solicitante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaPrestadorResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.OfertaWebMapper;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.SolicitanteWebMapper;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.ResolutorIdentidad;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ofertas del prestador: publicar y editar (RF-65 a RF-67). Todas las rutas van bajo {@code /prestador}
 * para autorizarlas en un solo lugar (DC-13) y exigen la identidad del prestador.
 */
@RestController
@RequestMapping("/prestador/ofertas")
public class OfertaPrestadorController {

    private final GestionarOfertaUseCase gestionarOferta;
    private final ResolutorIdentidad identidad;

    public OfertaPrestadorController(GestionarOfertaUseCase gestionarOferta, ResolutorIdentidad identidad) {
        this.gestionarOferta = gestionarOferta;
        this.identidad = identidad;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfertaPrestadorResponse publicar(@RequestBody OfertaRequest cuerpo, HttpServletRequest peticion) {
        Solicitante solicitante = solicitanteDe(peticion);
        return OfertaWebMapper.aRespuestaPrestador(gestionarOferta.publicar(
                new PublicarOfertaCommand(solicitante, OfertaWebMapper.aVersion(cuerpo))));
    }

    @PutMapping("/{id}")
    public OfertaPrestadorResponse editar(@PathVariable String id, @RequestBody OfertaRequest cuerpo,
                                          HttpServletRequest peticion) {
        Solicitante solicitante = solicitanteDe(peticion);
        return OfertaWebMapper.aRespuestaPrestador(gestionarOferta.editar(
                new EditarOfertaCommand(id, solicitante, OfertaWebMapper.aVersion(cuerpo))));
    }

    private Solicitante solicitanteDe(HttpServletRequest peticion) {
        return SolicitanteWebMapper.aSolicitante(identidad.prestadorDe(peticion));
    }
}
