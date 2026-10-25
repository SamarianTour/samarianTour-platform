package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoFuenteAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;

import java.util.List;

/**
 * Fuente externa de alertas del destino (RF-28). Cuál se usa se elige por configuración (DR-14).
 */
public interface WeatherAlertPort {

    /**
     * Las alertas de esas zonas que publica la fuente. Una fuente que consulta por ubicación usa sus coordenadas; una
     * que ya trae sus zonas puede ignorarlas.
     *
     * @return las alertas de las zonas leídas y las zonas que no se pudieron leer
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException
     *         si no se pudo leer ninguna zona
     */
    ResultadoFuenteAlertas consultar(List<ZonaCatalogo> zonas);
}
