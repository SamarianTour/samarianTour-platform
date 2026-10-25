package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertasDeZona;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResumenAlertas;

/**
 * Alertas activas del destino (RF-28). Lee solo la caché: la petición del turista nunca llama a la fuente (DR-16).
 */
public interface ConsultarAlertasZonaUseCase {

    /**
     * Las activas de la zona; vacía si no tiene. No comprueba que la zona exista en el catálogo.
     *
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.AlertasNoDisponiblesException
     *         si la fuente no ha respondido desde el arranque
     */
    AlertasDeZona deZona(String zonaId);

    /**
     * Todas las activas, agrupadas por zona y con su nombre si la caché del catálogo lo tiene.
     *
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.AlertasNoDisponiblesException
     *         si la fuente no ha respondido desde el arranque
     */
    ResumenAlertas todas();
}
