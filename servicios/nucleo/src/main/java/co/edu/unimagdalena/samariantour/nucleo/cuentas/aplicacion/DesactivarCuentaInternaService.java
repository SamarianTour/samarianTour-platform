package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.DesactivarCuentaInternaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Da de baja una cuenta interna, con sus sesiones, o la reactiva (RF-81). */
public class DesactivarCuentaInternaService implements DesactivarCuentaInternaUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final SesionRepositoryPort sesiones;
    private final Clock reloj;

    public DesactivarCuentaInternaService(UsuarioRepositoryPort usuarios, SesionRepositoryPort sesiones,
                                          Clock reloj) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Usuario desactivar(UUID usuarioId) {
        Usuario usuario = interna(usuarioId);
        if (usuario.desactivar()) {
            usuarios.actualizar(usuario);
            sesiones.revocarTodas(usuario.id(), reloj.instant());
        }
        return usuario;
    }

    @Override
    @Transactional
    public Usuario reactivar(UUID usuarioId) {
        Usuario usuario = interna(usuarioId);
        if (usuario.reactivar()) {
            usuarios.actualizar(usuario);
        }
        return usuario;
    }

    private Usuario interna(UUID usuarioId) {
        return usuarios.porId(usuarioId).filter(usuario -> usuario.rol().esInterno())
                .orElseThrow(() -> new CuentaNoEncontradaException(usuarioId));
    }
}
