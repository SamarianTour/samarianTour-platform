package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;

/**
 * Consulta del núcleo al crear una solicitud de disponibilidad (RF-30): valida la oferta y copia los datos que la
 * solicitud guarda. Solo se puede pedir una oferta visible.
 */
public interface ConsultarOfertaInternaUseCase {

    /**
     * @return la oferta, con su versión publicada
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException si no existe o no es visible
     */
    Oferta consultar(String ofertaId);
}
