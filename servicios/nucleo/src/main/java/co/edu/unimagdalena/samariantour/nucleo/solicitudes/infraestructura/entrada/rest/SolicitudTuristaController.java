package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarSolicitudesTuristaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.EnviarSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ResponderCondicionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.EnviarSolicitudCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.ResponderCondicionesCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.EnviarSolicitudRequest;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.RespuestaCondicionesRequest;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudResumenResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudTuristaResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;

/**
 * Las solicitudes del turista (RF-30 a RF-33, RF-42, RF-43), con el rol TURISTA (DN-05). Enviar y responder las
 * condiciones exigen el header {@code Idempotency-Key} (ADR-04, DN-12): repetir con la misma clave devuelve la
 * respuesta original.
 */
@RestController
@RequestMapping("/turista/solicitudes")
public class SolicitudTuristaController {

    private final EnviarSolicitudUseCase enviar;
    private final ConsultarSolicitudesTuristaUseCase consultar;
    private final ResponderCondicionesUseCase responder;
    private final RespuestasIdempotentes idempotentes;

    public SolicitudTuristaController(EnviarSolicitudUseCase enviar, ConsultarSolicitudesTuristaUseCase consultar,
                                      ResponderCondicionesUseCase responder, RespuestasIdempotentes idempotentes) {
        this.enviar = enviar;
        this.consultar = consultar;
        this.responder = responder;
        this.idempotentes = idempotentes;
    }

    @PostMapping
    public ResponseEntity<String> enviar(@RequestHeader(value = RespuestasIdempotentes.HEADER_CLAVE, required = false)
                                         String clave, @RequestBody EnviarSolicitudRequest peticion,
                                         HttpServletRequest http) {
        UUID turistaId = Identidad.de(http).usuarioId();
        PeticionIdempotente idempotente = idempotentes.peticion(clave, turistaId, "ENVIAR_SOLICITUD",
                "/turista/solicitudes", peticion);
        EnviarSolicitudCommand comando = new EnviarSolicitudCommand(turistaId, peticion.ofertaId(),
                peticion.fechaInicio(), peticion.horaSalida(), peticion.horaReserva(), peticion.fechaSalida(),
                peticion.numeroUnidades(), peticion.personas(), peticion.nota());
        return idempotentes.responder(enviar.enviar(comando, idempotente, solicitud ->
                idempotentes.guardar(HttpStatus.CREATED.value(), SolicitudWebMapper.aTurista(solicitud))));
    }

    @GetMapping
    public PaginaResponse<SolicitudResumenResponse> listar(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           HttpServletRequest http) {
        return SolicitudWebMapper.pagina(consultar.listar(Identidad.de(http).usuarioId(), new Paginacion(page, size)),
                SolicitudWebMapper::aResumen);
    }

    @GetMapping("/{solicitudId}")
    public SolicitudTuristaResponse consultar(@PathVariable UUID solicitudId, HttpServletRequest http) {
        return SolicitudWebMapper.aTurista(consultar.consultar(Identidad.de(http).usuarioId(), solicitudId));
    }

    @PostMapping("/{solicitudId}/respuesta")
    public ResponseEntity<String> responder(@PathVariable UUID solicitudId,
                                            @RequestHeader(value = RespuestasIdempotentes.HEADER_CLAVE,
                                                    required = false) String clave,
                                            @RequestBody RespuestaCondicionesRequest peticion,
                                            HttpServletRequest http) {
        UUID turistaId = Identidad.de(http).usuarioId();
        PeticionIdempotente idempotente = idempotentes.peticion(clave, turistaId, "RESPONDER_CONDICIONES",
                "/turista/solicitudes/" + solicitudId, peticion);
        ResponderCondicionesCommand comando = new ResponderCondicionesCommand(turistaId, solicitudId,
                decision(peticion.decision()));
        return idempotentes.responder(responder.responder(comando, idempotente, solicitud ->
                idempotentes.guardar(HttpStatus.OK.value(), SolicitudWebMapper.aTurista(solicitud))));
    }

    private static DecisionTurista decision(String valor) {
        return Arrays.stream(DecisionTurista.values())
                .filter(decision -> decision.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("decision", "debe ser CONFIRMAR o DECLINAR"));
    }
}
