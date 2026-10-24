package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración del proveedor de IA ({@code recomendacion.ia.*}). Cambiar de proveedor es cambiar url, modelo y key
 * (QAS-06, DR-01).
 *
 * @param habilitada           si es false, o no hay key, responde el motor por reglas
 * @param apiKey               sin valor por defecto: llega en la variable de entorno RECOMENDACION_IA_API_KEY
 * @param candidatas           tamaño de la lista corta que se envía al modelo (DR-04)
 * @param esfuerzoRazonamiento {@code reasoning_effort} para modelos de razonamiento; vacío para no enviarlo
 */
@ConfigurationProperties("recomendacion.ia")
public record PropiedadesIA(@DefaultValue("false") boolean habilitada,
                            @DefaultValue("https://api.groq.com/openai/v1") String url,
                            @DefaultValue("openai/gpt-oss-120b") String modelo,
                            String apiKey,
                            @DefaultValue("6s") Duration timeout,
                            @DefaultValue("0.2") double temperatura,
                            @DefaultValue("10") int candidatas,
                            @DefaultValue("low") String esfuerzoRazonamiento) {

    /** Sin la key: nunca debe llegar a un log. */
    @Override
    public String toString() {
        return "PropiedadesIA[habilitada=%s, url=%s, modelo=%s, apiKey=%s, timeout=%s]"
                .formatted(habilitada, url, modelo, apiKey == null || apiKey.isBlank() ? "-" : "(oculta)", timeout);
    }
}
