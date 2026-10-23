package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ListarPendientesModeracionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

/**
 * Ofertas que esperan la decisión del administrador (RF-79). Solo lee: la decisión llega como aviso del núcleo.
 */
public class ListarPendientesModeracionService implements ListarPendientesModeracionUseCase {

    private final OfertaRepositoryPort ofertas;

    public ListarPendientesModeracionService(OfertaRepositoryPort ofertas) {
        this.ofertas = ofertas;
    }

    @Override
    public Pagina<Oferta> listarOfertas(Paginacion paginacion) {
        return ofertas.buscarEnModeracion(paginacion);
    }

    @Override
    public Oferta consultarOferta(String ofertaId) {
        return ofertas.buscarPorId(ofertaId)
                .filter(Oferta::estaEnModeracion)
                .orElseThrow(() -> new OfertaNoEncontradaException(ofertaId));
    }
}
