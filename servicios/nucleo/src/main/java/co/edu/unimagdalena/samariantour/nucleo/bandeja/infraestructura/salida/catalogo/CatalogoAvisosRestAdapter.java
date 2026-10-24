package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.ResultadoEnvio;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.CatalogoAvisosPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Envía los avisos a las entradas internas del catálogo (DN-06), por la red de Docker y con un tiempo de espera de
 * 3 s (RNF-05).
 *
 * <ul>
 *   <li>2xx: recibido. El catálogo responde 200 tanto si lo aplicó como si ya lo tenía o no le correspondía.</li>
 *   <li>409: fallido. Es un conflicto de concurrencia que el catálogo no resolvió con sus reintentos (ADR-05); el
 *       mismo catálogo pide reenviarlo más tarde.</li>
 *   <li>Otro 4xx: rechazado. El aviso está mal formado y reintentar no lo arregla; queda anotado en el log.</li>
 *   <li>5xx, tiempo agotado o sin conexión: fallido, se reintenta.</li>
 * </ul>
 */
@Component
public class CatalogoAvisosRestAdapter implements CatalogoAvisosPort {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogoAvisosRestAdapter.class);

    private final RestClient catalogo;

    public CatalogoAvisosRestAdapter(RestClient catalogoAvisos) {
        this.catalogo = catalogoAvisos;
    }

    @Override
    public ResultadoEnvio enviar(Destino destino, String cuerpo) {
        try {
            catalogo.post().uri(ruta(destino)).contentType(MediaType.APPLICATION_JSON).body(cuerpo)
                    .retrieve().toBodilessEntity();
            return new ResultadoEnvio.Recibido();
        } catch (RestClientResponseException e) {
            return segunEstado(e.getStatusCode(), e.getResponseBodyAsString(), destino);
        } catch (RestClientException e) {
            return new ResultadoEnvio.Fallido("Sin respuesta del catálogo: " + e.getMessage());
        }
    }

    private static ResultadoEnvio segunEstado(HttpStatusCode estado, String cuerpo, Destino destino) {
        String motivo = estado.value() + " " + cuerpo;
        if (estado.is4xxClientError() && estado.value() != HttpStatus.CONFLICT.value()) {
            LOG.warn("El catálogo rechazó un aviso de {}: {}", destino, motivo);
            return new ResultadoEnvio.Rechazado(motivo);
        }
        return new ResultadoEnvio.Fallido(motivo);
    }

    static String ruta(Destino destino) {
        return switch (destino) {
            case OFERTAS -> "/internal/avisos";
            case OPINIONES -> "/internal/opiniones/avisos";
        };
    }
}
