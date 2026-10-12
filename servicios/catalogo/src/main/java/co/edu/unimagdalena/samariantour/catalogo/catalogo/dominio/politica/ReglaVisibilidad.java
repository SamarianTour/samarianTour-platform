package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;

/**
 * Una oferta es visible para el turista si tiene versión publicada, no está retirada y el RNT del prestador
 * está activo (RF-22, RF-65, RF-77). La búsqueda en MongoDB aplica las mismas tres condiciones en el filtro
 * del índice parcial de QAS-07.
 *
 * <p>Pendiente con los paquetes (ReglasPaquete, RF-22): un paquete además exige que todas sus actividades sean visibles.
 */
public final class ReglaVisibilidad {

    private ReglaVisibilidad() {
    }

    public static boolean esVisible(Oferta oferta) {
        return oferta.publicada().isPresent() && !oferta.retirada() && oferta.rntPrestadorActivo();
    }
}
