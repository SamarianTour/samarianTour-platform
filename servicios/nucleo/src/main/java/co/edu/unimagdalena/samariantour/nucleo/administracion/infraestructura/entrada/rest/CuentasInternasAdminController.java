package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.GestionarCuentasInternasUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.CuentaInternaRequest;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.CuentaInternaResponse;
import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * El administrador crea, consulta, desactiva y reactiva las cuentas internas (RF-01, RF-81, DN-40), con el rol
 * ADMINISTRADOR (DN-05). La cuenta nueva recibe por correo el enlace para poner su contraseña.
 */
@RestController
@RequestMapping("/admin/cuentas-internas")
public class CuentasInternasAdminController {

    private final GestionarCuentasInternasUseCase cuentas;

    public CuentasInternasAdminController(GestionarCuentasInternasUseCase cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping
    public List<CuentaInternaResponse> consultar() {
        return cuentas.consultar().stream().map(CuentasInternasAdminController::aRespuesta).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaInternaResponse crear(@RequestBody CuentaInternaRequest peticion) {
        return aRespuesta(cuentas.crear(new CrearCuentaInternaCommand(peticion.rol(), peticion.nombre(),
                peticion.correo())));
    }

    @PostMapping("/{usuarioId}/desactivacion")
    public CuentaInternaResponse desactivar(@PathVariable UUID usuarioId, HttpServletRequest http) {
        return aRespuesta(cuentas.desactivar(usuarioId, Identidad.de(http).usuarioId()));
    }

    @PostMapping("/{usuarioId}/reactivacion")
    public CuentaInternaResponse reactivar(@PathVariable UUID usuarioId) {
        return aRespuesta(cuentas.reactivar(usuarioId));
    }

    private static CuentaInternaResponse aRespuesta(CuentaInterna cuenta) {
        return new CuentaInternaResponse(cuenta.id(), cuenta.rol(), cuenta.nombre(), cuenta.correo(),
                cuenta.estado(), cuenta.contrasenaEstablecida(), cuenta.registradaEn());
    }
}
