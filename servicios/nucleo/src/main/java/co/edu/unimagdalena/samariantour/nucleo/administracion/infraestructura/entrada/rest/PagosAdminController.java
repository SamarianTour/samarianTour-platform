package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.CambioComisionResponse;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.ComisionRequest;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.ComisionResponse;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.PagoTardioResponse;
import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.ComisionConfigurada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.ComisionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.api.PagosAdministracionApi;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lo que el administrador gestiona de los pagos (RF-50, RF-82, RF-83, DN-23), con el rol ADMINISTRADOR (DN-05): la
 * comisión de la plataforma y los pagos tardíos que reembolsa a mano. Pagos es el dueño de esos datos; este controlador
 * los pide por {@code pagos.api}. El operador de disponibilidad nunca ve la comisión (SRS, perfil del operador).
 */
@RestController
@RequestMapping("/admin")
public class PagosAdminController {

    private final PagosAdministracionApi pagos;

    public PagosAdminController(PagosAdministracionApi pagos) {
        this.pagos = pagos;
    }

    @GetMapping("/comision")
    public ComisionResponse comision() {
        return aRespuesta(pagos.historialComision());
    }

    /** El nuevo porcentaje rige para los pagos que se confirmen desde ahora (RF-83). */
    @PutMapping("/comision")
    public ComisionResponse cambiarComision(@RequestBody ComisionRequest peticion, HttpServletRequest http) {
        try {
            pagos.actualizarComision(peticion.porcentaje(), Identidad.de(http).usuarioId());
        } catch (ComisionInvalidaException e) {
            String mensaje = e.getMessage();
            int separador = mensaje.indexOf(": ");
            throw separador < 0 ? new DatoInvalidoException("porcentaje", mensaje)
                    : new DatoInvalidoException(mensaje.substring(0, separador), mensaje.substring(separador + 2));
        }
        return aRespuesta(pagos.historialComision());
    }

    @GetMapping("/pagos-tardios")
    public List<PagoTardioResponse> pagosTardios() {
        return pagos.pagosTardios().stream()
                .map(pago -> new PagoTardioResponse(pago.id(), pago.solicitudId(), pago.referencia(),
                        pago.transaccionId(), pago.monto(), pago.motivo(), pago.registradoEn(), pago.avisado()))
                .toList();
    }

    private static ComisionResponse aRespuesta(List<ComisionConfigurada> historial) {
        ComisionConfigurada vigente = historial.getFirst();
        return new ComisionResponse(vigente.porcentaje(), vigente.vigenteDesde(), historial.stream()
                .map(cambio -> new CambioComisionResponse(cambio.porcentaje(), cambio.vigenteDesde(),
                        cambio.administradorId()))
                .toList());
    }
}
