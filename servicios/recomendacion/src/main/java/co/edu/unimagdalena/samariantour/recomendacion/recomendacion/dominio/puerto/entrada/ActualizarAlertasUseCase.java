package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada;

/**
 * Lee la fuente de alertas y guarda la lectura en la caché (RF-28, DR-16). Lo llama la tarea programada, al arrancar y
 * cada 8 horas.
 */
public interface ActualizarAlertasUseCase {

    /**
     * @return true si la fuente respondió; si falló, la caché conserva la última lectura buena y devuelve false
     */
    boolean actualizar();
}
