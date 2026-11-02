package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OrigenExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.ExposicionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * El {@code ExposicionRestClientAdapter} del diagrama (RET-02, DR-24): envía el registro a
 * {@code POST /internal/exposiciones} del núcleo con el token de servicio de la recomendación (DN-38). Si el núcleo no
 * lo recibe, lo deja en el log como antes (DR-08) y la recomendación igual llega al turista.
 */
public class ExposicionRestAdapter implements ExposicionPort {

    private static final Logger LOG = LoggerFactory.getLogger(ExposicionRestAdapter.class);

    private final RestClient nucleo;
    private final ExposicionPort respaldo;

    public ExposicionRestAdapter(RestClient nucleo, ExposicionPort respaldo) {
        this.nucleo = nucleo;
        this.respaldo = respaldo;
    }

    @Override
    public void registrar(RegistroExposicion registro) {
        try {
            nucleo.post().uri("/internal/exposiciones").contentType(MediaType.APPLICATION_JSON)
                    .body(ExposicionJson.de(registro)).retrieve().toBodilessEntity();
        } catch (Exception e) {
            LOG.warn("el núcleo no recibió la exposición, queda en el log: causa={}", e.getClass().getSimpleName());
            respaldo.registrar(registro);
        }
    }

    /** Lo que el núcleo guarda: sin los criterios, que no usa para medir el sesgo. */
    record ExposicionJson(String fecha, OrigenExposicion origen, MotorRecomendacion motor, List<OfertaJson> ofertas) {

        static ExposicionJson de(RegistroExposicion registro) {
            return new ExposicionJson(registro.fecha().toString(), registro.origen(), registro.motor(),
                    registro.ofertas().stream()
                            .map(o -> new OfertaJson(o.ofertaId(), o.prestadorId(), o.posicion()))
                            .toList());
        }
    }

    record OfertaJson(String ofertaId, UUID prestadorId, int posicion) {
    }
}
