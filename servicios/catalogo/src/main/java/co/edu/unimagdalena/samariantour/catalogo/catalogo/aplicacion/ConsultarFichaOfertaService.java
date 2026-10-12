package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFichaOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

/**
 * Ficha pública de una oferta (RF-20). Una oferta que no es visible responde igual que una inexistente.
 */
public class ConsultarFichaOfertaService implements ConsultarFichaOfertaUseCase {

    private final OfertaRepositoryPort ofertas;

    public ConsultarFichaOfertaService(OfertaRepositoryPort ofertas) {
        this.ofertas = ofertas;
    }

    @Override
    public Oferta consultar(String ofertaId) {
        return ofertas.buscarPorId(ofertaId)
                .filter(Oferta::estaVisible)
                .orElseThrow(() -> new OfertaNoEncontradaException(ofertaId));
    }
}
