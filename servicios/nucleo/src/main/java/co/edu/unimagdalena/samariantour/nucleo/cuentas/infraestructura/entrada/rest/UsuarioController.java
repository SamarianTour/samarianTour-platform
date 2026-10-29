package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.GestionarDatosPersonalesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RegistrarUsuarioUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SolicitarSupresionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.VerificarWhatsAppUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.ActualizarDatosCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.CuentaResponse;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.DatosPersonalesRequest;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.RegistroRequest;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.SupresionRequest;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto.VerificacionWhatsappRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * El registro, abierto a cualquiera (RF-01 a RF-03, RF-09 a RF-11); los datos de la propia cuenta, para cualquier rol
 * (RF-12); y la verificación del WhatsApp y la supresión, con el rol TURISTA (RF-03, RF-13, DN-05, DN-21).
 */
@RestController
public class UsuarioController {

    private final RegistrarUsuarioUseCase registrar;
    private final VerificarWhatsAppUseCase verificarWhatsApp;
    private final GestionarDatosPersonalesUseCase datosPersonales;
    private final SolicitarSupresionUseCase supresion;

    public UsuarioController(RegistrarUsuarioUseCase registrar, VerificarWhatsAppUseCase verificarWhatsApp,
                             GestionarDatosPersonalesUseCase datosPersonales, SolicitarSupresionUseCase supresion) {
        this.registrar = registrar;
        this.verificarWhatsApp = verificarWhatsApp;
        this.datosPersonales = datosPersonales;
        this.supresion = supresion;
    }

    @PostMapping("/cuentas/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaResponse registrar(@RequestBody RegistroRequest peticion) {
        return aRespuesta(registrar.registrar(new RegistrarUsuarioCommand(rol(peticion.rol()), peticion.nombre(),
                peticion.correo(), peticion.contrasena(), peticion.whatsapp(),
                Boolean.TRUE.equals(peticion.aceptaPolitica()), peticion.versionPolitica())));
    }

    /** Pide otro código: el anterior deja de servir. */
    @PostMapping("/turista/whatsapp/codigo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void enviarCodigo(HttpServletRequest http) {
        verificarWhatsApp.enviarCodigo(Identidad.de(http).usuarioId());
    }

    @PostMapping("/turista/whatsapp/verificacion")
    public CuentaResponse verificarWhatsApp(@RequestBody VerificacionWhatsappRequest peticion,
                                            HttpServletRequest http) {
        return aRespuesta(verificarWhatsApp.verificar(Identidad.de(http).usuarioId(), peticion.codigo()));
    }

    @GetMapping("/cuenta")
    public CuentaResponse consultar(HttpServletRequest http) {
        return aRespuesta(datosPersonales.consultar(Identidad.de(http).usuarioId()));
    }

    @PutMapping("/cuenta")
    public CuentaResponse actualizar(@RequestBody DatosPersonalesRequest peticion, HttpServletRequest http) {
        return aRespuesta(datosPersonales.actualizar(new ActualizarDatosCommand(Identidad.de(http).usuarioId(),
                peticion.nombre(), peticion.whatsapp())));
    }

    /** La supresión es inmediata: la cuenta queda anonimizada y la sesión, cerrada. */
    @PostMapping("/turista/supresion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suprimir(@RequestBody SupresionRequest peticion, HttpServletRequest http) {
        supresion.suprimir(Identidad.de(http).usuarioId(), peticion.contrasena());
    }

    private static Rol rol(String valor) {
        return Arrays.stream(Rol.values())
                .filter(rol -> rol.seRegistraSolo() && rol.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("rol", "debe ser TURISTA o PRESTADOR: las demás "
                        + "cuentas las crea el administrador (RF-01)"));
    }

    static CuentaResponse aRespuesta(Usuario usuario) {
        if (usuario instanceof Turista turista) {
            return new CuentaResponse(turista.id(), turista.rol().name(), turista.nombre(), turista.correo(),
                    turista.whatsapp(), turista.whatsappVerificado());
        }
        return new CuentaResponse(usuario.id(), usuario.rol().name(), usuario.nombre(), usuario.correo(), null,
                null);
    }
}
