package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.BuscarOfertasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

/**
 * Búsqueda pública de ofertas visibles (RF-14 a RF-17, RF-22). El filtro de visibilidad y el orden los resuelve
 * el repositorio sobre el índice de QAS-07.
 */
public class BuscarOfertasService implements BuscarOfertasUseCase {

    private final OfertaRepositoryPort ofertas;

    public BuscarOfertasService(OfertaRepositoryPort ofertas) {
        this.ofertas = ofertas;
    }

    @Override
    public Pagina<Oferta> buscar(CriteriosBusqueda criterios) {
        return ofertas.buscarVisibles(criterios);
    }
}
