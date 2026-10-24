package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OfertaEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OpinionEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.ModeracionCatalogoPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Consulta en el catálogo lo que se va a moderar, por la red interna y con la espera de 3 s del cliente compartido
 * (RNF-05): {@code GET /internal/moderacion/ofertas/{id}} y {@code GET /internal/moderacion/opiniones/{id}}. Un 404
 * es "no está en moderación"; cualquier otro error, o la falta de respuesta, es "catálogo no disponible" (DN-08).
 */
@Component
public class ModeracionCatalogoRestAdapter implements ModeracionCatalogoPort {

    /** Los campos de OfertaEnModeracion del contrato del catálogo que usa el núcleo. */
    record OfertaJson(String id, String nombre, UUID prestadorId, int numeroVersion) {
    }

    /** Los campos de OpinionEnModeracion del contrato del catálogo que usa el núcleo. */
    record OpinionJson(String id, String ofertaId, UUID turistaId) {
    }

    private final RestClient catalogo;

    public ModeracionCatalogoRestAdapter(RestClient clienteCatalogo) {
        this.catalogo = clienteCatalogo;
    }

    @Override
    public Optional<OfertaEnModeracion> ofertaEnModeracion(String ofertaId) {
        return consultar(() -> catalogo.get().uri("/internal/moderacion/ofertas/{id}", ofertaId).retrieve()
                .body(OfertaJson.class))
                .map(json -> new OfertaEnModeracion(json.id(), json.nombre(), json.prestadorId(),
                        json.numeroVersion()));
    }

    @Override
    public Optional<OpinionEnModeracion> opinionEnModeracion(String opinionId) {
        return consultar(() -> catalogo.get().uri("/internal/moderacion/opiniones/{id}", opinionId).retrieve()
                .body(OpinionJson.class))
                .map(json -> new OpinionEnModeracion(json.id(), json.ofertaId(), json.turistaId()));
    }

    private static <T> Optional<T> consultar(Supplier<T> consulta) {
        try {
            return Optional.ofNullable(consulta.get());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw new CatalogoNoDisponibleException("respondió " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new CatalogoNoDisponibleException("no respondió");
        }
    }
}
