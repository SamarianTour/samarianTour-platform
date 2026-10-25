package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración de la fuente del IDEAM ({@code recomendacion.alertas.ideam.*}, DR-14).
 *
 * @param url     la capa "Alertas_vigentes" del servicio ArcGIS del IDEAM, sin {@code /query}
 * @param timeout corte por consulta de cada zona
 */
@ConfigurationProperties("recomendacion.alertas.ideam")
public record PropiedadesIdeam(@DefaultValue(CAPA) String url, @DefaultValue("5s") Duration timeout) {

    /** Verificada el 24 de octubre de 2026. */
    static final String CAPA = "https://visualizador.ideam.gov.co/gisserver/rest/services/"
            + "StoryMaps_IDA/Alertas_Hidrologicas/MapServer/2";
}
