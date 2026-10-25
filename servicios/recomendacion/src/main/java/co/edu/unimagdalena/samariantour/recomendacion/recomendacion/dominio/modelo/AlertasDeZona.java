package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.time.Instant;
import java.util.List;

/**
 * Las alertas activas de una zona, en el orden para mostrar, con la hora de la lectura de la que salen (RF-28).
 *
 * @param alertas vacía si la zona no tiene alertas activas
 */
public record AlertasDeZona(String zonaId, Instant actualizadoEn, List<AlertaClimatica> alertas) {

    public AlertasDeZona {
        alertas = List.copyOf(alertas);
    }
}
