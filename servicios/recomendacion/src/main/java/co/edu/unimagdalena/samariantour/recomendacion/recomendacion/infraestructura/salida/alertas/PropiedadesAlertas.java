package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración de las alertas ({@code recomendacion.alertas.*}).
 *
 * @param fuente        {@code simulada} (por defecto) o {@code ideam} (DR-14)
 * @param actualizacion cada cuánto se lee la fuente: cada 8 horas, al arrancar y después (DR-16)
 * @param reintento     cada cuánto se reintenta mientras la caché no tenga ninguna lectura buena (DR-16)
 */
@ConfigurationProperties("recomendacion.alertas")
public record PropiedadesAlertas(@DefaultValue("simulada") String fuente,
                                 @DefaultValue("PT8H") Duration actualizacion,
                                 @DefaultValue("PT2M") Duration reintento) {

    public static final String SIMULADA = "simulada";
    public static final String IDEAM = "ideam";
}
