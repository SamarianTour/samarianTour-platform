package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.SesionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenOpaco;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RenovarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * La renovación con rotación del token de renovación (RF-04, DN-29). Un token que ya se usó y vuelve a llegar cierra
 * la sesión: o lo robaron, o lo usó otro cliente; en los dos casos hay que volver a iniciar sesión.
 */
public class RenovarSesionService implements RenovarSesionUseCase {

    private final SesionRepositoryPort sesiones;
    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort hasher;
    private final EmisionSesion emision;
    private final Clock reloj;

    public RenovarSesionService(SesionRepositoryPort sesiones, UsuarioRepositoryPort usuarios,
                                PasswordHasherPort hasher, EmisionSesion emision, Clock reloj) {
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.emision = emision;
        this.reloj = reloj;
    }

    /** Un token rechazado no deshace la transacción: el cierre de la sesión tiene que quedar guardado. */
    @Override
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public SesionIniciada renovar(String tokenRenovacion) {
        Instant ahora = reloj.instant();
        TokenOpaco token = TokenOpaco.leer(tokenRenovacion).orElseThrow(SesionInvalidaException::new);
        Sesion sesion = sesiones.porId(token.id())
                .filter(encontrada -> encontrada.estaVigente(ahora))
                .orElseThrow(SesionInvalidaException::new);
        if (!hasher.coincide(token.secreto(), sesion.hashRenovacion())) {
            cerrar(sesion, ahora);
        }
        Usuario usuario = usuarios.porId(sesion.usuarioId()).filter(Usuario::estaActiva).orElse(null);
        if (usuario == null) {
            cerrar(sesion, ahora);
        }
        return emision.renovar(sesion, usuario, ahora);
    }

    private void cerrar(Sesion sesion, Instant ahora) {
        sesion.revocar(ahora);
        sesiones.actualizar(sesion);
        throw new SesionInvalidaException();
    }
}
