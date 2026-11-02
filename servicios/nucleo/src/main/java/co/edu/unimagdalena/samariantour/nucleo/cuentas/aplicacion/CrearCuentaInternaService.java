package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenOpaco;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.CrearCuentaInternaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.EmailPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Crea una cuenta interna sin contraseña y le envía la invitación (RF-81, DN-40): un código
 * {@code ESTABLECER_CONTRASENA} que se usa con {@code POST /cuentas/contrasena}, como el del restablecimiento.
 */
public class CrearCuentaInternaService implements CrearCuentaInternaUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final CodigoRepositoryPort codigos;
    private final PasswordHasherPort hasher;
    private final EmailPort correo;
    private final RandomGenerator azar;
    private final Clock reloj;

    public CrearCuentaInternaService(UsuarioRepositoryPort usuarios, CodigoRepositoryPort codigos,
                                     PasswordHasherPort hasher, EmailPort correo, RandomGenerator azar, Clock reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.hasher = hasher;
        this.correo = correo;
        this.azar = azar;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Usuario crear(CrearCuentaInternaCommand comando) {
        Instant ahora = reloj.instant();
        Usuario usuario = Usuario.crearInterna(UUID.randomUUID(), comando.rol(), comando.nombre(), comando.correo(),
                ahora);
        usuarios.insertar(usuario);
        UUID codigoId = UUID.randomUUID();
        TokenOpaco token = TokenOpaco.nuevo(codigoId, azar);
        codigos.insertar(CodigoUnUso.emitir(codigoId, usuario.id(), PropositoCodigo.ESTABLECER_CONTRASENA,
                hasher.hash(token.secreto()), ahora));
        correo.enviarEstablecimiento(usuario.correo(), usuario.nombre(), token.valor());
        return usuario;
    }
}
