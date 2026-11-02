package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.AsignarOperadorPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.AsignacionOperadorRequest;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.PrestadorAdminResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * El administrador ve los prestadores y les asigna el operador de disponibilidad (RF-32, RF-81, DN-39), con el rol
 * ADMINISTRADOR (DN-05). Con {@code ?sinOperador=true}, solo los que faltan por asignar.
 */
@RestController
@RequestMapping("/admin/prestadores")
public class PrestadoresAdminController {

    private final AsignarOperadorPrestadorUseCase asignacion;

    public PrestadoresAdminController(AsignarOperadorPrestadorUseCase asignacion) {
        this.asignacion = asignacion;
    }

    @GetMapping
    public List<PrestadorAdminResponse> prestadores(@RequestParam(defaultValue = "false") boolean sinOperador) {
        return asignacion.prestadores(sinOperador).stream().map(PrestadoresAdminController::aRespuesta).toList();
    }

    @PutMapping("/{prestadorId}/operador")
    public PrestadorAdminResponse asignarOperador(@PathVariable UUID prestadorId,
                                                  @RequestBody AsignacionOperadorRequest peticion) {
        if (peticion.operadorId() == null) {
            throw new DatoInvalidoException("operadorId", "es obligatorio");
        }
        return aRespuesta(asignacion.asignar(prestadorId, peticion.operadorId()));
    }

    private static PrestadorAdminResponse aRespuesta(PrestadorRegistrado prestador) {
        return new PrestadorAdminResponse(prestador.id(), prestador.numeroRnt(), prestador.razonSocial(),
                prestador.tipo(), prestador.estadoRnt(), prestador.operadorId(), prestador.registradoEn());
    }
}
