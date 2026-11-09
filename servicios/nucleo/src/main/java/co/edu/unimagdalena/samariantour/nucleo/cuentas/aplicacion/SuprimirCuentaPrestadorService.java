package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SuprimirCuentaPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * La cuenta en la supresión del prestador (RF-13, DN-45), con las mismas reglas que la del turista (DN-31): se confirma
 * con la contraseña, el fallo cuenta para el bloqueo, y la cuenta queda anonimizada y sin sesiones.
 */
public class SuprimirCuentaPrestadorService implements SuprimirCuentaPrestadorUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final SesionRepositoryPort sesiones;
    private final PasswordHasherPort hasher;
    private final Clock reloj;

    public SuprimirCuentaPrestadorService(UsuarioRepositoryPort usuarios, SesionRepositoryPort sesiones,
                                          PasswordHasherPort hasher, Clock reloj) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.hasher = hasher;
        this.reloj = reloj;
    }

    /** En su propia transacción, que no se deshace por la contraseña equivocada: el fallo queda contado (RF-05). */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {ContrasenaIncorrectaException.class, CuentaBloqueadaException.class})
    public void verificarContrasena(UUID prestadorId, String contrasena) {
        Instant ahora = reloj.instant();
        Prestador prestador = prestadorActivo(prestadorId);
        if (prestador.estaBloqueada(ahora)) {
            throw new CuentaBloqueadaException(prestador.bloqueadaHasta());
        }
        if (!prestador.autenticar(contrasena, hasher)) {
            prestador.registrarIntentoFallido(ahora);
            usuarios.actualizar(prestador);
            if (prestador.estaBloqueada(ahora)) {
                throw new CuentaBloqueadaException(prestador.bloqueadaHasta());
            }
            throw new ContrasenaIncorrectaException();
        }
    }

    @Override
    @Transactional
    public void suprimir(UUID prestadorId) {
        Instant ahora = reloj.instant();
        Prestador prestador = prestadorActivo(prestadorId);
        prestador.suprimir(UUID.randomUUID(), ahora);
        usuarios.actualizar(prestador);
        sesiones.revocarTodas(prestadorId, ahora);
    }

    private Prestador prestadorActivo(UUID prestadorId) {
        return usuarios.porId(prestadorId)
                .filter(Prestador.class::isInstance).map(Prestador.class::cast)
                .filter(Prestador::estaActiva)
                .orElseThrow(() -> new CuentaNoEncontradaException(prestadorId));
    }
}
