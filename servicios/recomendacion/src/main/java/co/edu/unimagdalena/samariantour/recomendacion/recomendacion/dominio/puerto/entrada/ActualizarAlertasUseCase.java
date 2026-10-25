package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada;

/**
 * Lee la fuente de alertas y guarda la lectura en la caché (RF-28, DR-16). Lo llama la tarea programada: al arrancar,
 * cada 8 horas y, mientras no haya ninguna lectura buena, cada 2 minutos.
 */
public interface ActualizarAlertasUseCase {

    /**
     * @return true si la fuente respondió; si falló, la caché conserva la última lectura buena y devuelve false
     */
    boolean actualizar();

    /**
     * Lee la fuente solo si la caché todavía no tiene ninguna lectura buena, por ejemplo porque la del arranque falló.
     * Con una lectura en la caché no hace nada: sigue el ciclo de {@link #actualizar()}.
     */
    Reintento reintentar();

    enum Reintento {
        /** Ya hay una lectura buena: no se llamó a la fuente. */
        NO_HACE_FALTA,
        /** No había lectura y la fuente respondió. */
        ACTUALIZADA,
        /** No había lectura y la fuente volvió a fallar (o su circuito estaba abierto). */
        FALLIDA
    }
}
