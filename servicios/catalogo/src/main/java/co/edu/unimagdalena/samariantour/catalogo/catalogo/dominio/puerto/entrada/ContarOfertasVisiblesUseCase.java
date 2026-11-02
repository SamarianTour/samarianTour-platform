package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertasDePrestador;

import java.util.List;

/**
 * La oferta elegible de cada prestador: el núcleo la compara con sus apariciones en las recomendaciones para medir el
 * sesgo (RET-02, DC-51).
 */
public interface ContarOfertasVisiblesUseCase {

    /** Solo los prestadores con al menos una oferta visible, de más a menos ofertas. */
    List<OfertasDePrestador> porPrestador();
}
