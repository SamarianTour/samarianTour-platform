package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenOpaco;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SolicitarRestablecimientoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.EmailPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Envía el enlace para restablecer la contraseña (RF-07). El enlace se guarda como un {@link CodigoUnUso} con el hash
 * de su secreto, en la misma transacción en que se envía el correo: si el correo falla, no queda nada.
 */
public class SolicitarRestablecimientoService implements SolicitarRestablecimientoUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final CodigoRepositoryPort codigos;
    private final PasswordHasherPort hasher;
    private final EmailPort correo;
    private final RandomGenerator azar;
    private final Clock reloj;

    public SolicitarRestablecimientoService(UsuarioRepositoryPort usuarios, CodigoRepositoryPort codigos,
                                            PasswordHasherPort hasher, EmailPort correo, RandomGenerator azar,
                                            Clock reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.hasher = hasher;
        this.correo = correo;
        this.azar = azar;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void solicitar(String direccion) {
        String normalizada = direccion == null ? "" : direccion.strip().toLowerCase(Locale.ROOT);
        Optional<Usuario> cuenta = usuarios.porCorreo(normalizada).filter(Usuario::estaActiva);
        UUID id = UUID.randomUUID();
        TokenOpaco token = TokenOpaco.nuevo(id, azar);
        // El hash se calcula aunque la cuenta no exista: así la respuesta tarda lo mismo y no la delata.
        String hash = hasher.hash(token.secreto());
        cuenta.ifPresent(usuario -> {
            Instant ahora = reloj.instant();
            codigos.insertar(CodigoUnUso.emitir(id, usuario.id(), PropositoCodigo.RESTABLECER_CONTRASENA, hash,
                    ahora));
            correo.enviarRestablecimiento(usuario.correo(), usuario.nombre(), token.valor());
        });
    }
}
