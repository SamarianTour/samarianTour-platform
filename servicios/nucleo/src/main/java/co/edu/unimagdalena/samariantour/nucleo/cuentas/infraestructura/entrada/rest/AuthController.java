package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.CerrarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.IniciarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RenovarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RestablecerContrasenaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SolicitarRestablecimientoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.InicioSesionRequest;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.NuevaContrasenaRequest;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.SesionResponse;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.SolicitudRestablecimientoRequest;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.TokenRenovacionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iniciar, renovar y cerrar la sesión (RF-04 a RF-06, DN-19) y restablecer la contraseña (RF-07, RF-08, DN-20). Son
 * rutas públicas: quien llega aquí todavía no tiene token, o el suyo venció.
 */
@RestController
public class AuthController {

    private final IniciarSesionUseCase iniciar;
    private final RenovarSesionUseCase renovar;
    private final CerrarSesionUseCase cerrar;
    private final SolicitarRestablecimientoUseCase solicitarRestablecimiento;
    private final RestablecerContrasenaUseCase restablecer;

    public AuthController(IniciarSesionUseCase iniciar, RenovarSesionUseCase renovar, CerrarSesionUseCase cerrar,
                          SolicitarRestablecimientoUseCase solicitarRestablecimiento,
                          RestablecerContrasenaUseCase restablecer) {
        this.iniciar = iniciar;
        this.renovar = renovar;
        this.cerrar = cerrar;
        this.solicitarRestablecimiento = solicitarRestablecimiento;
        this.restablecer = restablecer;
    }

    @PostMapping("/cuentas/sesion")
    public SesionResponse iniciar(@RequestBody InicioSesionRequest peticion) {
        return aRespuesta(iniciar.iniciar(peticion.correo(), peticion.contrasena()));
    }

    @PostMapping("/cuentas/sesion/renovacion")
    public SesionResponse renovar(@RequestBody TokenRenovacionRequest peticion) {
        return aRespuesta(renovar.renovar(peticion.tokenRenovacion()));
    }

    @PostMapping("/cuentas/sesion/cierre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrar(@RequestBody TokenRenovacionRequest peticion) {
        cerrar.cerrar(peticion.tokenRenovacion());
    }

    /** Responde 202 exista o no el correo: no revela qué correos están registrados (RF-07). */
    @PostMapping("/cuentas/contrasena/restablecimiento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitarRestablecimiento(@RequestBody SolicitudRestablecimientoRequest peticion) {
        solicitarRestablecimiento.solicitar(peticion.correo());
    }

    @PostMapping("/cuentas/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@RequestBody NuevaContrasenaRequest peticion) {
        restablecer.restablecer(peticion.token(), peticion.contrasena());
    }

    private static SesionResponse aRespuesta(SesionIniciada sesion) {
        return new SesionResponse(sesion.acceso().valor(), "Bearer", sesion.acceso().expiraEn(),
                sesion.renovacion().valor(), sesion.renovacionExpiraEn(),
                UsuarioController.aRespuesta(sesion.usuario()));
    }
}
