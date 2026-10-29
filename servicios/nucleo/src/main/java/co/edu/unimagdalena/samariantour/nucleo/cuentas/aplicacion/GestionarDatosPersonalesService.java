package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.EstadoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.GestionarDatosPersonalesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.ActualizarDatosCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Consulta y actualización de los datos personales del titular (RF-12). */
public class GestionarDatosPersonalesService implements GestionarDatosPersonalesUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final EmisionCodigoWhatsapp emision;
    private final Clock reloj;

    public GestionarDatosPersonalesService(UsuarioRepositoryPort usuarios, EmisionCodigoWhatsapp emision,
                                           Clock reloj) {
        this.usuarios = usuarios;
        this.emision = emision;
        this.reloj = reloj;
    }

    @Override
    public Usuario consultar(UUID usuarioId) {
        return cuenta(usuarioId);
    }

    /** El nombre, el WhatsApp y su código nuevo se guardan juntos. */
    @Override
    @Transactional
    public Usuario actualizar(ActualizarDatosCommand comando) {
        Usuario usuario = cuenta(comando.usuarioId());
        usuario.cambiarNombre(comando.nombre());
        if (usuario instanceof Turista turista) {
            if (turista.cambiarWhatsapp(comando.whatsapp())) {
                emision.emitir(turista, reloj.instant());
            }
        } else if (comando.whatsapp() != null && !comando.whatsapp().isBlank()) {
            throw new DatoInvalidoException("whatsapp", "solo lo registra el turista; el del prestador va en sus "
                    + "datos de prestador");
        }
        usuarios.actualizar(usuario);
        return usuario;
    }

    private Usuario cuenta(UUID usuarioId) {
        return usuarios.porId(usuarioId)
                .filter(usuario -> usuario.estado() != EstadoCuenta.SUPRIMIDA)
                .orElseThrow(() -> new CuentaNoEncontradaException(usuarioId));
    }
}
