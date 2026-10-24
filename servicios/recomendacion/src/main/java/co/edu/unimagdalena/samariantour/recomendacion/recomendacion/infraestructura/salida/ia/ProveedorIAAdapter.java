package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.ia;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.MotorNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RespuestaIAInvalidaException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.PropuestaIA;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.PoliticaRotacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ValidacionRespuestaIA;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.RecommendationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor con IA (RF-27, QAS-06): pide a un modelo, por la API compatible con la de OpenAI
 * ({@code POST {url}/chat/completions}), que elija y explique de 3 a 5 ofertas de una lista corta.
 * <ul>
 *   <li>Lista corta: las de mayor afinidad, como máximo {@code candidatas} (10), barajadas para que la posición no
 *   sesgue al modelo (DR-04).</li>
 *   <li>La respuesta pasa por {@link ValidacionRespuestaIA}: se acepta completa o se descarta completa (DR-05).</li>
 *   <li>Tiempo agotado, 429, 5xx, cualquier otro rechazo o respuesta inválida son fallas: el servicio responde con el
 *   motor por reglas. Tras varias fallas seguidas, el corte de circuito va directo a reglas por un rato (DR-06).</li>
 * </ul>
 * En el log va el motor, la latencia y la causa de una falla; nunca el texto del turista, el prompt ni la key.
 */
public class ProveedorIAAdapter implements RecommendationPort {

    private static final Logger LOG = LoggerFactory.getLogger(ProveedorIAAdapter.class);

    private final RestClient proveedor;
    private final PropiedadesIA propiedades;
    private final CircuitBreaker circuito;
    private final ObjectMapper json;
    private final ValidacionRespuestaIA validacion;
    private final PoliticaRotacion rotacion;

    /**
     * @param proveedor cliente con la url base, la key y el tiempo máximo ya configurados
     */
    public ProveedorIAAdapter(RestClient proveedor, PropiedadesIA propiedades, CircuitBreaker circuito,
                              ObjectMapper json, ValidacionRespuestaIA validacion, PoliticaRotacion rotacion) {
        this.proveedor = proveedor;
        this.propiedades = propiedades;
        this.circuito = circuito;
        this.json = json;
        this.validacion = validacion;
        this.rotacion = rotacion;
    }

    @Override
    public MotorRecomendacion motor() {
        return MotorRecomendacion.IA;
    }

    @Override
    public List<Recomendacion> recomendar(Preferencias preferencias, List<OfertaEvaluada> candidatas) {
        List<OfertaEvaluada> listaCorta = rotacion.barajar(
                candidatas.subList(0, Math.min(propiedades.candidatas(), candidatas.size())));
        long inicio = System.nanoTime();
        try {
            List<Recomendacion> recomendaciones = circuito.executeSupplier(() -> consultar(preferencias, listaCorta));
            LOG.info("motor=IA modelo={} recomendaciones={} latenciaMs={}", propiedades.modelo(),
                    recomendaciones.size(), milisDesde(inicio));
            return recomendaciones;
        } catch (CallNotPermittedException e) {
            LOG.warn("motor=IA falla causa=CIRCUITO_ABIERTO latenciaMs={}", milisDesde(inicio));
            throw new MotorNoDisponibleException("CIRCUITO_ABIERTO");
        } catch (MotorNoDisponibleException e) {
            LOG.warn("motor=IA falla causa={} latenciaMs={}", e.causa(), milisDesde(inicio));
            throw e;
        }
    }

    private List<Recomendacion> consultar(Preferencias preferencias, List<OfertaEvaluada> listaCorta) {
        String respuesta = enviar(peticion(preferencias, listaCorta));
        try {
            return validacion.validar(leer(respuesta), listaCorta, preferencias);
        } catch (RespuestaIAInvalidaException e) {
            throw new MotorNoDisponibleException("RESPUESTA_INVALIDA (" + e.getMessage() + ")");
        }
    }

    private Map<String, Object> peticion(Preferencias preferencias, List<OfertaEvaluada> listaCorta) {
        Map<String, Object> peticion = new LinkedHashMap<>();
        peticion.put("model", propiedades.modelo());
        peticion.put("temperature", propiedades.temperatura());
        peticion.put("response_format", Map.of("type", "json_object"));
        if (propiedades.esfuerzoRazonamiento() != null && !propiedades.esfuerzoRazonamiento().isBlank()) {
            peticion.put("reasoning_effort", propiedades.esfuerzoRazonamiento());
        }
        try {
            peticion.put("messages", List.of(
                    Map.of("role", "system", "content", PromptIA.sistema(preferencias.idioma())),
                    Map.of("role", "user", "content",
                            json.writeValueAsString(PromptIA.datos(preferencias, listaCorta)))));
        } catch (JsonProcessingException e) {
            throw new MotorNoDisponibleException("PETICION_INVALIDA");
        }
        return peticion;
    }

    private String enviar(Map<String, Object> peticion) {
        try {
            return proveedor.post().uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(peticion)
                    .retrieve()
                    .body(String.class);
        } catch (HttpStatusCodeException e) {
            int estado = e.getStatusCode().value();
            throw new MotorNoDisponibleException(estado == 429 ? "LIMITE_429"
                    : e.getStatusCode().is5xxServerError() ? "ERROR_PROVEEDOR_" + estado
                    : "RECHAZO_" + estado);
        } catch (ResourceAccessException e) {
            throw new MotorNoDisponibleException(esTiempoAgotado(e) ? "TIMEOUT" : "SIN_CONEXION");
        } catch (RestClientException e) {
            throw new MotorNoDisponibleException("ERROR_CLIENTE");
        }
    }

    /** Lee {@code choices[0].message.content}, que a su vez es el JSON pedido. */
    private PropuestaIA leer(String respuesta) {
        try {
            String contenido = json.readTree(respuesta == null ? "" : respuesta)
                    .path("choices").path(0).path("message").path("content").asText("");
            JsonNode propuesta = json.readTree(sinCercas(contenido));
            JsonNode lista = propuesta == null ? null : propuesta.get("recomendaciones");
            if (lista == null || !lista.isArray()) {
                return new PropuestaIA(null);
            }
            List<PropuestaIA.Item> items = new ArrayList<>();
            for (JsonNode item : lista) {
                List<String> criterios = null;
                if (item.path("criterios").isArray()) {
                    criterios = new ArrayList<>();
                    for (JsonNode criterio : item.get("criterios")) {
                        criterios.add(criterio.asText());
                    }
                }
                items.add(new PropuestaIA.Item(texto(item, "ofertaId"), criterios, texto(item, "explicacion")));
            }
            return new PropuestaIA(items);
        } catch (JsonProcessingException e) {
            throw new MotorNoDisponibleException("RESPUESTA_INVALIDA (JSON mal formado)");
        }
    }

    /** Algunos modelos envuelven el JSON en un bloque de código aunque se pida JSON. */
    private static String sinCercas(String contenido) {
        String texto = contenido.strip();
        if (texto.startsWith("```")) {
            texto = texto.substring(texto.indexOf('\n') + 1);
            texto = texto.endsWith("```") ? texto.substring(0, texto.length() - 3) : texto;
        }
        return texto.strip();
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    private static boolean esTiempoAgotado(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static long milisDesde(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }
}
