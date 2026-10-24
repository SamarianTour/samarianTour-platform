package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.ExposicionPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Escribe una línea JSON por consulta en el logger {@code exposicion}: fecha, motor y, por cada recomendación, la
 * oferta, su prestador, la posición y los criterios (RET-02, DR-08). Con eso se puede contar cuántas veces y en qué
 * posición sale cada prestador. No lleva el texto ni ningún dato del turista.
 * <p>Provisional: cuando exista el núcleo, se cambia por un adaptador HTTP que le envíe el mismo registro.
 */
@Component
public class ExposicionLogAdapter implements ExposicionPort {

    private static final Logger EXPOSICION = LoggerFactory.getLogger("exposicion");
    private static final Logger LOG = LoggerFactory.getLogger(ExposicionLogAdapter.class);

    private final ObjectMapper json;
    private final Consumer<String> destino;

    @Autowired
    public ExposicionLogAdapter(ObjectMapper json) {
        this(json, EXPOSICION::info);
    }

    ExposicionLogAdapter(ObjectMapper json, Consumer<String> destino) {
        this.json = json;
        this.destino = destino;
    }

    @Override
    public void registrar(RegistroExposicion registro) {
        try {
            destino.accept(json.writeValueAsString(LineaExposicion.de(registro)));
        } catch (Exception e) {
            // La recomendación ya está lista: un registro fallido no debe impedir que llegue al turista.
            LOG.warn("no se pudo registrar la exposicion: causa={}", e.getClass().getSimpleName());
        }
    }

    record LineaExposicion(String fecha, MotorRecomendacion motor, List<OfertaJson> recomendaciones) {

        static LineaExposicion de(RegistroExposicion registro) {
            return new LineaExposicion(registro.fecha().toString(), registro.motor(), registro.ofertas().stream()
                    .map(o -> new OfertaJson(o.ofertaId(), o.prestadorId(), o.posicion(), o.criterios()))
                    .toList());
        }
    }

    record OfertaJson(String ofertaId, UUID prestadorId, int posicion, List<CriterioAfinidad> criterios) {
    }
}
