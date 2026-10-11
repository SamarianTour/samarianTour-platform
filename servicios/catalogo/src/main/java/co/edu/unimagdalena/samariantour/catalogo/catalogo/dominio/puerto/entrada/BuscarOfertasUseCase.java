package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;

/**
 * Búsqueda pública de ofertas visibles (RF-14 a RF-17, RF-22).
 */
public interface BuscarOfertasUseCase {

    Pagina<Oferta> buscar(CriteriosBusqueda criterios);
}
