package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenOpaco;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Validaciones;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RestablecerContrasenaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * Pone la contraseña nueva con el enlace del correo (RF-08). El enlace sirve si es el último que se pidió, no venció y
 * no se usó; al usarlo se cierran todas las sesiones, por si alguien más conocía la contraseña anterior.
 */
public class RestablecerContrasenaService implements RestablecerContrasenaUseCase {

    /** Los códigos que ponen una contraseña: restablecerla (RF-07) o la primera de una cuenta interna (RF-81). */
    private static final Set<PropositoCodigo> DE_CONTRASENA =
            Set.of(PropositoCodigo.RESTABLECER_CONTRASENA, PropositoCodigo.ESTABLECER_CONTRASENA);

    private final UsuarioRepositoryPort usuarios;
    private final CodigoRepositoryPort codigos;
    private final SesionRepositoryPort sesiones;
    private final PasswordHasherPort hasher;
    private final Clock reloj;

    public RestablecerContrasenaService(UsuarioRepositoryPort usuarios, CodigoRepositoryPort codigos,
                                        SesionRepositoryPort sesiones, PasswordHasherPort hasher, Clock reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.sesiones = sesiones;
        this.hasher = hasher;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void restablecer(String token, String contrasenaNueva) {
        Validaciones.contrasena(contrasenaNueva);
        Instant ahora = reloj.instant();
        TokenOpaco enlace = TokenOpaco.leer(token).orElseThrow(CodigoNoVigenteException::new);
        CodigoUnUso codigo = codigos.porId(enlace.id())
                .filter(encontrado -> DE_CONTRASENA.contains(encontrado.proposito()))
                .filter(encontrado -> encontrado.esValido(ahora))
                .filter(this::esElUltimo)
                .filter(encontrado -> hasher.coincide(enlace.secreto(), encontrado.hashCodigo()))
                .orElseThrow(CodigoNoVigenteException::new);
        Usuario usuario = usuarios.porId(codigo.usuarioId()).filter(Usuario::estaActiva)
                .orElseThrow(CodigoNoVigenteException::new);
        codigo.usar(ahora);
        codigos.actualizar(codigo);
        usuario.cambiarContrasena(hasher.hash(contrasenaNueva));
        usuarios.actualizar(usuario);
        sesiones.revocarTodas(usuario.id(), ahora);
    }

    /** Pedir un enlace nuevo invalida los anteriores. */
    private boolean esElUltimo(CodigoUnUso codigo) {
        return codigos.ultimo(codigo.usuarioId(), codigo.proposito())
                .map(ultimo -> ultimo.id().equals(codigo.id()))
                .orElse(false);
    }
}
