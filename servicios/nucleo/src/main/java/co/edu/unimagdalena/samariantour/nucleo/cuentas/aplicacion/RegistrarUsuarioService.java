package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.PoliticaNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Validaciones;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RegistrarUsuarioUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PoliticaDatosRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * El registro de un turista o un prestador (RF-01 a RF-03, RF-09 a RF-11). La cuenta, la aceptación de la política y
 * el código del WhatsApp se guardan en la misma transacción.
 */
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final PoliticaDatosRepositoryPort politicas;
    private final PasswordHasherPort hasher;
    private final EmisionCodigoWhatsapp emision;
    private final Clock reloj;

    public RegistrarUsuarioService(UsuarioRepositoryPort usuarios, PoliticaDatosRepositoryPort politicas,
                                   PasswordHasherPort hasher, EmisionCodigoWhatsapp emision, Clock reloj) {
        this.usuarios = usuarios;
        this.politicas = politicas;
        this.hasher = hasher;
        this.emision = emision;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Usuario registrar(RegistrarUsuarioCommand comando) {
        if (!comando.aceptaPolitica()) {
            throw new DatoInvalidoException("aceptaPolitica",
                    "hay que aceptar la política de tratamiento de datos para registrarse (RF-09)");
        }
        PoliticaDatos vigente = politicas.vigente();
        if (!vigente.version().equals(comando.versionPolitica())) {
            throw new PoliticaNoVigenteException(comando.versionPolitica(), vigente.version());
        }
        Validaciones.contrasena(comando.contrasena());
        Instant ahora = reloj.instant();
        Usuario usuario = Usuario.registrar(UUID.randomUUID(), comando.rol(), comando.nombre(), comando.correo(),
                hasher.hash(comando.contrasena()), comando.whatsapp(), vigente, ahora);
        usuarios.insertar(usuario);
        if (usuario instanceof Turista turista) {
            emision.emitir(turista, ahora);
        }
        return usuario;
    }
}
