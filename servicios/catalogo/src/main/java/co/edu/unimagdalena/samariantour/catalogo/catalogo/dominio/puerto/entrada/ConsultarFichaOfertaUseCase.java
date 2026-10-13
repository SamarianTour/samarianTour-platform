package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FichaOferta;

/**
 * Ficha pública de una oferta (RF-20), con su zona (RF-25) y el perfil público del prestador (RF-24).
 * Las opiniones (RF-21) quedan fuera del prototipo.
 */
public interface ConsultarFichaOfertaUseCase {

    /** @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException si no existe o no es visible */
    FichaOferta consultar(String ofertaId);
}
