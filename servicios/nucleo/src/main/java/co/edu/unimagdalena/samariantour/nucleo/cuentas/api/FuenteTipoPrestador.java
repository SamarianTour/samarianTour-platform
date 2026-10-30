package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

import java.util.Optional;
import java.util.UUID;

/**
 * El tipo del prestador, que va en su token de acceso para que el catálogo sepa qué ofertas puede publicar (DN-37).
 * Lo sabe el módulo de prestadores, que la implementa. Las cuentas emiten el token, pero no pueden depender de
 * prestadores, que ya depende de ellas: sería un ciclo. Por eso declaran aquí lo que necesitan y prestadores lo aporta.
 */
public interface FuenteTipoPrestador {

    /** El nombre del tipo ({@code ALOJAMIENTO}, ...); vacío si todavía no registró su RNT. */
    Optional<String> tipoDe(UUID prestadorId);
}
