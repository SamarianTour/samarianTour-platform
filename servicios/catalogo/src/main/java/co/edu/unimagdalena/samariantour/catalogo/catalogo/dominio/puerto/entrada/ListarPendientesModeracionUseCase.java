package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;

/**
 * Lo que espera la decisión del administrador (RF-79). El administrador lo lee aquí y decide en el núcleo, que envía
 * la decisión al catálogo como aviso de moderación (DC-04, DC-38). Las opiniones pendientes (RF-80) las lista el
 * módulo de opiniones.
 */
public interface ListarPendientesModeracionUseCase {

    /** Ofertas en moderación, de la enviada hace más tiempo a la más reciente. */
    Pagina<Oferta> listarOfertas(Paginacion paginacion);

    /**
     * Una oferta en moderación, para que el núcleo aplique sus reglas de aprobación y notifique al prestador.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException si no existe o no está en moderación
     */
    Oferta consultarOferta(String ofertaId);
}
