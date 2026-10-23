package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertaVisible;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;

/**
 * Ofertas visibles que la recomendación usa como base de conocimiento cerrada (RF-26) y para proponer alternativas
 * (RF-29). El orden es el de la búsqueda pública, por precio y luego por id: el catálogo no prioriza a ningún
 * prestador, y la rotación entre ofertas de igual afinidad le corresponde a la recomendación (RET-02).
 */
public interface ConsultarOfertasVisiblesUseCase {

    /**
     * @param tipo   solo ofertas de ese tipo; null para todas
     * @param zonaId solo ofertas de esa zona; null para todas
     */
    Pagina<OfertaVisible> consultar(TipoOferta tipo, String zonaId, Paginacion paginacion);
}
