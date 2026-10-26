package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DatosOfertaCopiados;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OfertaSolicitable;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.CatalogoConsultaPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La oferta desde {@code GET /internal/ofertas/{id}} del catálogo, por la red interna (RF-30, DN-09), con el cliente
 * compartido (espera de 3 s, RNF-05) y el circuito {@code catalogo} de Resilience4j (ADR-04, QAS-03):
 * <ul>
 *   <li>200: la oferta; 404: no está visible, y no cuenta como falla del circuito;</li>
 *   <li>otro error, tiempo agotado, sin conexión o una respuesta que no se entiende: catálogo no disponible;</li>
 *   <li>con el circuito abierto no se llama al catálogo: responde no disponible al instante.</li>
 * </ul>
 */
@Component
public class CatalogoConsultaRestAdapter implements CatalogoConsultaPort {

    static final String CIRCUITO = "catalogo";
    private static final Logger LOG = LoggerFactory.getLogger(CatalogoConsultaRestAdapter.class);

    /** Los campos de OfertaInterna del contrato del catálogo. */
    record OfertaJson(String id, String tipo, String nombre, UUID prestadorId, String zonaId,
                      List<String> horariosSalida, Integer capacidadMaxima) {
    }

    private final RestClient catalogo;
    private final CircuitBreaker circuito;

    @Autowired
    public CatalogoConsultaRestAdapter(RestClient clienteCatalogo, CircuitBreakerRegistry circuitos) {
        this(clienteCatalogo, circuitos.circuitBreaker(CIRCUITO));
    }

    CatalogoConsultaRestAdapter(RestClient clienteCatalogo, CircuitBreaker circuito) {
        this.catalogo = clienteCatalogo;
        this.circuito = circuito;
    }

    @Override
    public Optional<OfertaSolicitable> oferta(String ofertaId) {
        try {
            return circuito.executeSupplier(() -> consultar(ofertaId));
        } catch (CallNotPermittedException e) {
            throw new CatalogoNoDisponibleException("su circuito está abierto tras varias fallas seguidas");
        } catch (RestClientResponseException e) {
            LOG.warn("catalogo: la oferta {} respondió {}", ofertaId, e.getStatusCode().value());
            throw new CatalogoNoDisponibleException("respondió " + e.getStatusCode().value());
        } catch (RestClientException e) {
            LOG.warn("catalogo: la oferta {} no respondió: {}", ofertaId, e.getClass().getSimpleName());
            throw new CatalogoNoDisponibleException("no respondió a tiempo");
        } catch (RespuestaInvalida e) {
            LOG.warn("catalogo: la oferta {} llegó con un formato inesperado: {}", ofertaId, e.getMessage());
            throw new CatalogoNoDisponibleException("respondió con un formato inesperado");
        }
    }

    /** Un 404 es una respuesta válida: no es una falla para el circuito. */
    private Optional<OfertaSolicitable> consultar(String ofertaId) {
        OfertaJson json;
        try {
            json = catalogo.get().uri("/internal/ofertas/{id}", ofertaId).retrieve().body(OfertaJson.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw e;
        }
        return Optional.of(aDominio(json));
    }

    private static OfertaSolicitable aDominio(OfertaJson json) {
        if (json == null || json.id() == null || json.capacidadMaxima() == null) {
            throw new RespuestaInvalida("faltan campos obligatorios");
        }
        try {
            List<LocalTime> horarios = json.horariosSalida() == null ? List.of()
                    : json.horariosSalida().stream().map(LocalTime::parse).toList();
            return new OfertaSolicitable(json.id(), new DatosOfertaCopiados(TipoOferta.valueOf(json.tipo()),
                    json.nombre(), json.prestadorId(), json.zonaId()), horarios, json.capacidadMaxima());
        } catch (IllegalArgumentException | NullPointerException | DateTimeParseException e) {
            throw new RespuestaInvalida(e.getMessage());
        }
    }

    /** El catálogo respondió 200 con algo que no corresponde a su contrato; cuenta como falla del circuito. */
    private static final class RespuestaInvalida extends RuntimeException {
        RespuestaInvalida(String mensaje) {
            super(mensaje);
        }
    }
}
