package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada;

/**
 * Envía al catálogo un lote de avisos pendientes cuyo intento ya toca (DN-06). Las réplicas pueden publicar a la vez:
 * cada una toma avisos que la otra no tiene tomados.
 */
public interface PublicarAvisosUseCase {

    /** @return cuántos avisos tomó; 0 si no había ninguno por enviar */
    int publicarLote();
}
