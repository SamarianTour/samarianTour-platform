package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;

import java.util.UUID;

/**
 * Las opiniones propias del turista en cualquier estado, de la más reciente a la más antigua, con el motivo si fue
 * rechazada (DC-42).
 */
public interface ListarOpinionesTuristaUseCase {

    Pagina<OpinionConOferta> listar(UUID turistaId, Paginacion paginacion);
}
