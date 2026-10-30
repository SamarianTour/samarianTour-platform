package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import java.util.Optional;
import java.util.UUID;

/** El tipo del prestador, para su token de acceso (DN-37); lo aporta el módulo de prestadores. */
public interface TipoPrestadorPort {

    /** Vacío si todavía no registró su RNT. */
    Optional<String> tipoDe(UUID prestadorId);
}
