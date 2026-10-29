package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenAcceso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;

import java.time.Instant;
import java.util.UUID;

/** Firma el token de acceso, JWT RS256 de vida corta (RF-04, DN-29). */
public interface TokenPort {

    /** Un token con el usuario, su rol y la sesión, que vence en {@code Sesion.VIGENCIA_ACCESO}. */
    TokenAcceso emitirAcceso(Usuario usuario, UUID sesionId, Instant ahora);
}
