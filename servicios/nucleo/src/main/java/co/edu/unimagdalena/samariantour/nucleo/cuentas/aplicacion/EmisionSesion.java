package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenAcceso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenOpaco;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TipoPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TokenPort;

import java.time.Instant;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Abre o renueva una sesión y emite sus dos tokens (DN-29). Lo usan el inicio de sesión y la renovación; corre dentro
 * de la transacción de quien lo llama. Del token de renovación solo se guarda el hash.
 *
 * <p>El token del prestador lleva su tipo, leído en cada emisión (DN-37): después de registrar su RNT o de cambiar de
 * tipo, renovar la sesión basta para que el token lo traiga.
 */
public class EmisionSesion {

    private final SesionRepositoryPort sesiones;
    private final PasswordHasherPort hasher;
    private final TokenPort tokens;
    private final TipoPrestadorPort tiposPrestador;
    private final RandomGenerator azar;

    public EmisionSesion(SesionRepositoryPort sesiones, PasswordHasherPort hasher, TokenPort tokens,
                         TipoPrestadorPort tiposPrestador, RandomGenerator azar) {
        this.sesiones = sesiones;
        this.hasher = hasher;
        this.tokens = tokens;
        this.tiposPrestador = tiposPrestador;
        this.azar = azar;
    }

    SesionIniciada abrir(Usuario usuario, Instant ahora) {
        UUID id = UUID.randomUUID();
        TokenOpaco renovacion = TokenOpaco.nuevo(id, azar);
        Sesion sesion = Sesion.iniciar(id, usuario.id(), hasher.hash(renovacion.secreto()), ahora);
        sesiones.insertar(sesion);
        return new SesionIniciada(usuario, acceso(usuario, id, ahora), renovacion, sesion.expiraEn());
    }

    SesionIniciada renovar(Sesion sesion, Usuario usuario, Instant ahora) {
        TokenOpaco renovacion = TokenOpaco.nuevo(sesion.id(), azar);
        sesion.renovar(hasher.hash(renovacion.secreto()), ahora);
        sesiones.actualizar(sesion);
        return new SesionIniciada(usuario, acceso(usuario, sesion.id(), ahora), renovacion, sesion.expiraEn());
    }

    private TokenAcceso acceso(Usuario usuario, UUID sesionId, Instant ahora) {
        String tipo = usuario.rol() == Rol.PRESTADOR ? tiposPrestador.tipoDe(usuario.id()).orElse(null) : null;
        return tokens.emitirAcceso(usuario, tipo, sesionId, ahora);
    }
}
