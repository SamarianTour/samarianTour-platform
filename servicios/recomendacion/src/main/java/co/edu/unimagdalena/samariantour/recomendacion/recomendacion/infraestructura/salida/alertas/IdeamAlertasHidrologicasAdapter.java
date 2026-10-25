package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoFuenteAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.WeatherAlertPort;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Alertas hidrológicas vigentes del IDEAM, por su servicio ArcGIS REST: la capa "Alertas_vigentes" (DR-14, DR-15).
 * <ul>
 *   <li>Una consulta por zona, con el punto de su ubicación ({@code geometry=longitud,latitud}). Sin
 *   {@code returnGeometry}, y solo con los campos {@code NOMSZH} (subzona hidrográfica) y {@code NIVEL_A}.</li>
 *   <li>Alerta amarilla, naranja o roja: una alerta HIDROLOGICA de la zona. Condiciones normales, ninguna feature o un
 *   nivel desconocido: sin alerta (el desconocido queda en el log). Si caen varias features en el punto, queda la de
 *   mayor nivel.</li>
 *   <li>La capa no tiene fechas: la alerta va sin emisión ni vigencia, y lo que se muestra es la hora de la consulta.</li>
 *   <li>Si una zona falla (tiempo agotado, 5xx, error de ArcGIS o respuesta ilegible), las demás siguen y esa conserva
 *   sus alertas anteriores. Si fallan todas, la fuente falla y se conserva la lectura completa.</li>
 * </ul>
 * El servicio está publicado bajo "StoryMaps_IDA", no como una API documentada: puede cambiar sin aviso (DR-14).
 */
public class IdeamAlertasHidrologicasAdapter implements WeatherAlertPort {

    static final String FUENTE = "IDEAM, alertas hidrológicas vigentes";
    private static final Logger LOG = LoggerFactory.getLogger(IdeamAlertasHidrologicasAdapter.class);
    private static final Map<String, NivelAlerta> NIVELES = Map.of(
            "ALERTA AMARILLA", NivelAlerta.AMARILLA,
            "ALERTA NARANJA", NivelAlerta.NARANJA,
            "ALERTA ROJA", NivelAlerta.ROJA);
    private static final String CONDICIONES_NORMALES = "CONDICIONES NORMALES";

    private final RestClient ideam;
    private final CircuitBreaker circuito;

    /**
     * @param ideam cliente con la url de la capa como base y el tiempo máximo ya configurado
     */
    public IdeamAlertasHidrologicasAdapter(RestClient ideam, CircuitBreaker circuito) {
        this.ideam = ideam;
        this.circuito = circuito;
    }

    @Override
    public ResultadoFuenteAlertas consultar(List<ZonaCatalogo> zonas) {
        List<ZonaCatalogo> consultables = zonas.stream().filter(ZonaCatalogo::tieneUbicacion).toList();
        if (consultables.isEmpty()) {
            LOG.warn("alertas ideam sin consultar: no hay zonas con ubicacion");
            throw new FuenteAlertasNoDisponibleException("SIN_ZONAS");
        }
        long inicio = System.nanoTime();
        List<AlertaClimatica> alertas = new ArrayList<>();
        Set<String> fallidas = new HashSet<>();
        String ultimaCausa = null;
        for (ZonaCatalogo zona : consultables) {
            try {
                AlertaClimatica alerta = circuito.executeSupplier(() -> consultarZona(zona));
                if (alerta != null) {
                    alertas.add(alerta);
                }
            } catch (CallNotPermittedException e) {
                fallidas.add(zona.id());
                ultimaCausa = "CIRCUITO_ABIERTO";
            } catch (FuenteAlertasNoDisponibleException e) {
                fallidas.add(zona.id());
                ultimaCausa = e.causa();
                LOG.warn("alertas ideam: zona={} sin leer causa={}", zona.id(), e.causa());
            }
        }
        long latencia = (System.nanoTime() - inicio) / 1_000_000;
        if (fallidas.size() == consultables.size()) {
            LOG.warn("alertas ideam: ninguna zona se pudo leer causa={} latenciaMs={}", ultimaCausa, latencia);
            throw new FuenteAlertasNoDisponibleException(ultimaCausa);
        }
        LOG.info("alertas ideam: zonas={} conAlerta={} fallidas={} latenciaMs={}", consultables.size(),
                alertas.size(), fallidas.size(), latencia);
        return new ResultadoFuenteAlertas(alertas, fallidas);
    }

    /** null si la zona no tiene alerta. */
    private AlertaClimatica consultarZona(ZonaCatalogo zona) {
        JsonNode respuesta = pedir(zona);
        if (respuesta == null || respuesta.has("error") || !respuesta.path("features").isArray()) {
            throw new FuenteAlertasNoDisponibleException(respuesta != null && respuesta.has("error")
                    ? "ERROR_ARCGIS_" + respuesta.path("error").path("code").asText("?")
                    : "RESPUESTA_INVALIDA");
        }
        NivelAlerta mayor = null;
        String subzona = null;
        for (JsonNode feature : respuesta.path("features")) {
            String nivelTexto = feature.path("attributes").path("NIVEL_A").asText("").strip()
                    .toUpperCase(Locale.ROOT);
            NivelAlerta nivel = NIVELES.get(nivelTexto);
            if (nivel == null) {
                if (!nivelTexto.equals(CONDICIONES_NORMALES)) {
                    LOG.warn("alertas ideam: zona={} con un nivel desconocido, se toma como sin alerta: '{}'",
                            zona.id(), nivelTexto);
                }
                continue;
            }
            if (mayor == null || nivel.compareTo(mayor) > 0) {
                mayor = nivel;
                subzona = feature.path("attributes").path("NOMSZH").asText("").strip().replaceAll("\\s+", " ");
            }
        }
        return mayor == null ? null : alerta(zona, mayor, subzona);
    }

    private JsonNode pedir(ZonaCatalogo zona) {
        // ArcGIS recibe el punto como x,y: primero la longitud y después la latitud.
        String punto = String.format(Locale.ROOT, "%s,%s", zona.longitud(), zona.latitud());
        try {
            return ideam.get()
                    .uri(uri -> uri.path("/query")
                            .queryParam("geometry", punto)
                            .queryParam("geometryType", "esriGeometryPoint")
                            .queryParam("inSR", "4326")
                            .queryParam("spatialRel", "esriSpatialRelIntersects")
                            .queryParam("outFields", "NOMSZH,NIVEL_A")
                            .queryParam("returnGeometry", "false")
                            .queryParam("f", "json")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException e) {
            throw new FuenteAlertasNoDisponibleException("HTTP_" + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new FuenteAlertasNoDisponibleException(esTiempoAgotado(e) ? "TIMEOUT" : "SIN_CONEXION");
        } catch (RestClientException e) {
            throw new FuenteAlertasNoDisponibleException("RESPUESTA_INVALIDA");
        }
    }

    private static AlertaClimatica alerta(ZonaCatalogo zona, NivelAlerta nivel, String subzona) {
        String color = nivel.name().toLowerCase(Locale.ROOT);
        boolean conSubzona = subzona != null && !subzona.isBlank();
        String donde = conSubzona ? "en la subzona hidrográfica " + subzona + ", donde queda esta zona" : "en esta zona";
        return new AlertaClimatica("ideam-hidrologica-" + zona.id(), zona.id(), TipoAlerta.HIDROLOGICA, nivel,
                conSubzona ? "Alerta hidrológica en la subzona " + subzona : "Alerta hidrológica",
                "El IDEAM tiene vigente una alerta hidrológica %s %s. ".formatted(color, donde)
                        + "Antes de visitar ríos y quebradas, consulta las indicaciones de las autoridades locales.",
                FUENTE, null, null);
    }

    private static boolean esTiempoAgotado(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
