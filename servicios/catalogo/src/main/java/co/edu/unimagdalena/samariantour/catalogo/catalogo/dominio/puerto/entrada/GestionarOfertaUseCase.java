package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.PublicarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;

import java.util.UUID;

/**
 * Publicar, editar y retirar ofertas del prestador (RF-65 a RF-67).
 */
public interface GestionarOfertaUseCase {

    Oferta publicar(PublicarOfertaCommand comando);

    Oferta editar(EditarOfertaCommand comando);

    void retirar(RetirarOfertaCommand comando);

    /** Ofertas del prestador en cualquier estado, incluidas las retiradas. */
    Pagina<Oferta> listarMias(UUID prestadorId, Paginacion paginacion);
}
