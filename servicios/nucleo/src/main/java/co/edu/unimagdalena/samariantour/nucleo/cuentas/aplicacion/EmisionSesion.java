package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenOpaco;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TokenPort;

import java.time.Instant;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Abre o renueva una sesión y emite sus dos tokens (DN-19). Lo usan el inicio de sesión y la renovación; corre dentro
 * de la transacción de quien lo llama. Del token de renovación solo se guarda el hash.
 */
public class EmisionSesion {

    private final SesionRepositoryPort sesiones;
    private final PasswordHasherPort hasher;
    private final TokenPort tokens;
    private final RandomGenerator azar;

    public EmisionSesion(SesionRepositoryPort sesiones, PasswordHasherPort hasher, TokenPort tokens,
                         RandomGenerator azar) {
        this.sesiones = sesiones;
        this.hasher = hasher;
        this.tokens = tokens;
        this.azar = azar;
    }

    SesionIniciada abrir(Usuario usuario, Instant ahora) {
        UUID id = UUID.randomUUID();
        TokenOpaco renovacion = TokenOpaco.nuevo(id, azar);
        Sesion sesion = Sesion.iniciar(id, usuario.id(), hasher.hash(renovacion.secreto()), ahora);
        sesiones.insertar(sesion);
        return new SesionIniciada(usuario, tokens.emitirAcceso(usuario, id, ahora), renovacion, sesion.expiraEn());
    }

    SesionIniciada renovar(Sesion sesion, Usuario usuario, Instant ahora) {
        TokenOpaco renovacion = TokenOpaco.nuevo(sesion.id(), azar);
        sesion.renovar(hasher.hash(renovacion.secreto()), ahora);
        sesiones.actualizar(sesion);
        return new SesionIniciada(usuario, tokens.emitirAcceso(usuario, sesion.id(), ahora), renovacion,
                sesion.expiraEn());
    }
}
