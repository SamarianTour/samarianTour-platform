package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesTuristaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;

import java.util.UUID;

/**
 * "Mis opiniones": las del turista en cualquier estado, con el nombre de cada oferta y el motivo si fue rechazada.
 */
public class ListarOpinionesTuristaService implements ListarOpinionesTuristaUseCase {

    private final OpinionRepositoryPort opiniones;
    private final NombresDeOfertas nombres;

    public ListarOpinionesTuristaService(OpinionRepositoryPort opiniones, OfertaCatalogoPort ofertas) {
        this.opiniones = opiniones;
        this.nombres = new NombresDeOfertas(ofertas);
    }

    @Override
    public Pagina<OpinionConOferta> listar(UUID turistaId, Paginacion paginacion) {
        return nombres.de(opiniones.buscarDelTurista(turistaId, paginacion));
    }
}
