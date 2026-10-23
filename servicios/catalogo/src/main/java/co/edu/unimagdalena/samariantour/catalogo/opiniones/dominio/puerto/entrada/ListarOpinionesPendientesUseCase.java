package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;

/**
 * Lo que espera la decisión del administrador (RF-80). Como con las ofertas (DC-38), el administrador lo lee aquí y
 * decide en el núcleo, que envía la decisión al catálogo como aviso (DC-44).
 */
public interface ListarOpinionesPendientesUseCase {

    /** Opiniones en moderación, de la enviada hace más tiempo a la más reciente y luego por id. */
    Pagina<OpinionConOferta> listar(Paginacion paginacion);

    /**
     * Una opinión en moderación, para que el núcleo sepa a quién notificar antes de decidir.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoEncontradaException si no existe o no está en moderación
     */
    Opinion consultar(String opinionId);
}
