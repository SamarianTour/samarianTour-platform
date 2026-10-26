package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoAtencion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.AtenderSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarColaOperadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.AtenderSolicitudCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.AtencionRequest;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.AvisoResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudColaResponse;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto.SolicitudOperadorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;

/**
 * La cola del operador de disponibilidad y la atención de sus solicitudes (RF-34 a RF-40), con el rol
 * OPERADOR_DISPONIBILIDAD (DN-05). Solo ve y atiende las que tiene asignadas: la de otro operador responde 404.
 */
@RestController
@RequestMapping("/operador")
public class SolicitudOperadorController {

    private final ConsultarColaOperadorUseCase cola;
    private final AtenderSolicitudUseCase atender;
    private final Clock reloj;

    public SolicitudOperadorController(ConsultarColaOperadorUseCase cola, AtenderSolicitudUseCase atender,
                                       Clock reloj) {
        this.cola = cola;
        this.atender = atender;
        this.reloj = reloj;
    }

    /** Sin estado, su cola: las pendientes de la más antigua a la más reciente (RF-35). */
    @GetMapping("/solicitudes")
    public PaginaResponse<SolicitudColaResponse> cola(@RequestParam(defaultValue = "PENDIENTE") String estado,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      HttpServletRequest http) {
        var ahora = reloj.instant();
        return SolicitudWebMapper.pagina(cola.cola(operador(http), estado(estado), new Paginacion(page, size)),
                solicitud -> SolicitudWebMapper.aCola(solicitud, ahora));
    }

    @GetMapping("/solicitudes/{solicitudId}")
    public SolicitudOperadorResponse detalle(@PathVariable UUID solicitudId, HttpServletRequest http) {
        return SolicitudWebMapper.aOperador(cola.detalle(operador(http), solicitudId), reloj.instant());
    }

    @PostMapping("/solicitudes/{solicitudId}/atencion")
    public SolicitudOperadorResponse atender(@PathVariable UUID solicitudId, @RequestBody AtencionRequest peticion,
                                             HttpServletRequest http) {
        UUID operadorId = operador(http);
        atender.atender(new AtenderSolicitudCommand(operadorId, solicitudId, resultado(peticion.resultado()),
                peticion.precioBase(), peticion.politicaCancelacion(), peticion.aparteAcordado(), peticion.nota()));
        return SolicitudWebMapper.aOperador(cola.detalle(operadorId, solicitudId), reloj.instant());
    }

    @GetMapping("/avisos")
    public PaginaResponse<AvisoResponse> avisos(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                HttpServletRequest http) {
        return SolicitudWebMapper.pagina(cola.avisos(operador(http), new Paginacion(page, size)),
                SolicitudWebMapper::aAviso);
    }

    private static UUID operador(HttpServletRequest http) {
        return Identidad.de(http).usuarioId();
    }

    private static EstadoSolicitud estado(String valor) {
        return Arrays.stream(EstadoSolicitud.values())
                .filter(estado -> estado.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("estado", "debe ser uno de "
                        + Arrays.toString(EstadoSolicitud.values())));
    }

    private static ResultadoAtencion resultado(String valor) {
        return Arrays.stream(ResultadoAtencion.values())
                .filter(resultado -> resultado.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("resultado", "debe ser ATENDIDA o NO_DISPONIBLE"));
    }
}
