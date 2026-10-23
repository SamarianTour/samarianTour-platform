package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.PublicarOpinionCommand;

/**
 * El turista con una solicitud pagada de la oferta deja su opinión, que queda en moderación (RF-60, RF-61).
 */
public interface PublicarOpinionUseCase {

    /**
     * @return la opinión en moderación, con el nombre de su oferta
     * @throws co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException si la calificación o el comentario no cumplen las reglas
     * @throws co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OfertaNoEncontradaException si la oferta no existe
     * @throws co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoHabilitadaException si el turista no la pagó
     * @throws co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException si ya opinó sobre ella
     */
    OpinionConOferta publicar(PublicarOpinionCommand comando);
}
