package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.LecturaAlertas;

import java.util.Optional;

/**
 * La última lectura buena de la fuente de alertas, en memoria: el servicio no tiene base de datos (DC-32, DR-16).
 */
public interface AlertaCachePort {

    void guardar(LecturaAlertas lectura);

    /** Vacía si la fuente no ha respondido desde el arranque. */
    Optional<LecturaAlertas> ultima();
}
