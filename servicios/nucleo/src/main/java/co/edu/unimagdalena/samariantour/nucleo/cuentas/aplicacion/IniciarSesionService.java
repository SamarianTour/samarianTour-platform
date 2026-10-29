package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CredencialesInvalidasException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaDesactivadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.IniciarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * El inicio de sesión con el bloqueo por intentos fallidos (RF-04, RF-05). La cuenta se lee bloqueando su fila, así
 * que los intentos de una misma cuenta se atienden uno tras otro y ninguno se escapa del conteo.
 */
public class IniciarSesionService implements IniciarSesionUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort hasher;
    private final EmisionSesion emision;
    private final Clock reloj;
    /** Para comparar contra algo cuando el correo no existe: así la respuesta tarda lo mismo y no lo delata. */
    private final String hashFicticio;

    public IniciarSesionService(UsuarioRepositoryPort usuarios, PasswordHasherPort hasher, EmisionSesion emision,
                                Clock reloj) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.emision = emision;
        this.reloj = reloj;
        this.hashFicticio = hasher.hash("contrasena-ficticia-1");
    }

    /** Un fallo o el bloqueo no deshacen la transacción: el intento tiene que quedar contado. */
    @Override
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, CuentaBloqueadaException.class})
    public SesionIniciada iniciar(String correo, String contrasena) {
        Instant ahora = reloj.instant();
        String normalizado = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = usuarios.porCorreoParaActualizar(normalizado);
        if (encontrado.isEmpty()) {
            hasher.coincide(contrasena == null ? "" : contrasena, hashFicticio);
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = encontrado.get();
        if (usuario.estaBloqueada(ahora)) {
            throw new CuentaBloqueadaException(usuario.bloqueadaHasta());
        }
        if (!usuario.autenticar(contrasena, hasher)) {
            usuario.registrarIntentoFallido(ahora);
            usuarios.actualizar(usuario);
            if (usuario.estaBloqueada(ahora)) {
                throw new CuentaBloqueadaException(usuario.bloqueadaHasta());
            }
            throw new CredencialesInvalidasException();
        }
        if (!usuario.estaActiva()) {
            throw new CuentaDesactivadaException();
        }
        if (usuario.intentosFallidos() > 0 || usuario.bloqueadaHasta() != null) {
            usuario.registrarIngreso();
            usuarios.actualizar(usuario);
        }
        return emision.abrir(usuario, ahora);
    }
}
