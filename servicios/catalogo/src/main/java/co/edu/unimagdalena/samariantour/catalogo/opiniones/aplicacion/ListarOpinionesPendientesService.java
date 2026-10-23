package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesPendientesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;

/**
 * Opiniones que esperan la decisión del administrador (RF-80, DC-43). Solo lee: la decisión llega como aviso del
 * núcleo.
 */
public class ListarOpinionesPendientesService implements ListarOpinionesPendientesUseCase {

    private final OpinionRepositoryPort opiniones;
    private final NombresDeOfertas nombres;

    public ListarOpinionesPendientesService(OpinionRepositoryPort opiniones, OfertaCatalogoPort ofertas) {
        this.opiniones = opiniones;
        this.nombres = new NombresDeOfertas(ofertas);
    }

    @Override
    public Pagina<OpinionConOferta> listar(Paginacion paginacion) {
        return nombres.de(opiniones.buscarEnModeracion(paginacion));
    }

    @Override
    public Opinion consultar(String opinionId) {
        return opiniones.buscarPorId(opinionId)
                .filter(Opinion::estaEnModeracion)
                .orElseThrow(() -> new OpinionNoEncontradaException(opinionId));
    }
}
