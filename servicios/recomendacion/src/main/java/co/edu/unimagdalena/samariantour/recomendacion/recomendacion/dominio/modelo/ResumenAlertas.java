package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.time.Instant;
import java.util.List;

/**
 * Todas las alertas activas, agrupadas por zona, para la vista general (RF-28).
 *
 * @param zonas solo las zonas con al menos una alerta activa
 */
public record ResumenAlertas(Instant actualizadoEn, List<Zona> zonas) {

    public ResumenAlertas {
        zonas = List.copyOf(zonas);
    }

    /**
     * @param zonaNombre null si la caché del catálogo no lo tiene
     */
    public record Zona(String zonaId, String zonaNombre, List<AlertaClimatica> alertas) {

        public Zona {
            alertas = List.copyOf(alertas);
        }
    }
}
