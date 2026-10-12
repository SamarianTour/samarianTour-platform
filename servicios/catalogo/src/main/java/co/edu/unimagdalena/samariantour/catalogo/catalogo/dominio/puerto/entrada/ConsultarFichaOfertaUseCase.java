package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;

/**
 * Ficha pública de una oferta (RF-20). Las opiniones (RF-21) quedan fuera del prototipo.
 */
public interface ConsultarFichaOfertaUseCase {

    /** @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException si no existe o no es visible */
    Oferta consultar(String ofertaId);
}
