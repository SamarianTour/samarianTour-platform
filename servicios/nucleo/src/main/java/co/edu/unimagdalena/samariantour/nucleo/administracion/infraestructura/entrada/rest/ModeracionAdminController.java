package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOfertaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOpinionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOfertaCommand;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOpinionCommand;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.DecisionModeracionResponse;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.DecisionOfertaRequest;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.DecisionOpinionRequest;
import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;

/**
 * El administrador decide lo que espera moderación (RF-79, RF-80, DN-08), con el rol ADMINISTRADOR (DN-05). Lee las
 * colas en el catálogo ({@code GET /api/catalogo/admin/moderacion/...}) y decide aquí; el núcleo avisa al catálogo.
 */
@RestController
@RequestMapping("/admin/moderacion")
public class ModeracionAdminController {

    private final ModerarOfertaUseCase moderarOferta;
    private final ModerarOpinionUseCase moderarOpinion;

    public ModeracionAdminController(ModerarOfertaUseCase moderarOferta, ModerarOpinionUseCase moderarOpinion) {
        this.moderarOferta = moderarOferta;
        this.moderarOpinion = moderarOpinion;
    }

    @PostMapping("/ofertas/{ofertaId}/decision")
    public DecisionModeracionResponse moderarOferta(@PathVariable String ofertaId,
                                                    @RequestBody DecisionOfertaRequest peticion,
                                                    HttpServletRequest http) {
        if (peticion.numeroVersion() == null || peticion.numeroVersion() < 1) {
            throw new DatoInvalidoException("numeroVersion", "es obligatorio y empieza en 1");
        }
        return aRespuesta(moderarOferta.moderar(new ModerarOfertaCommand(ofertaId, peticion.numeroVersion(),
                decision(peticion.decision()), peticion.motivo(), administrador(http))));
    }

    @PostMapping("/opiniones/{opinionId}/decision")
    public DecisionModeracionResponse moderarOpinion(@PathVariable String opinionId,
                                                     @RequestBody DecisionOpinionRequest peticion,
                                                     HttpServletRequest http) {
        return aRespuesta(moderarOpinion.moderar(new ModerarOpinionCommand(opinionId, decision(peticion.decision()),
                peticion.motivo(), administrador(http))));
    }

    private static Decision decision(String valor) {
        return Arrays.stream(Decision.values())
                .filter(decision -> decision.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("decision", "debe ser APROBAR o RECHAZAR"));
    }

    private static UUID administrador(HttpServletRequest http) {
        return Identidad.de(http).usuarioId();
    }

    private static DecisionModeracionResponse aRespuesta(DecisionModeracion decision) {
        return new DecisionModeracionResponse(decision.tipo(), decision.moderadoId(), decision.numeroVersion(),
                decision.decision(), decision.motivo(), decision.administradorId(), decision.decididaEn());
    }
}
