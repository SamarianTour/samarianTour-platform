package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.reglas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.RecommendationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor por reglas (RF-27, DR-01): toma las 5 de mayor afinidad, ya rotadas entre empates, y explica cada una con los
 * criterios que cumple, por plantilla. No depende de servicios externos, así que nunca falla por ellos: es el respaldo
 * cuando la IA no responde (QAS-03).
 */
public class RecomendadorPorReglasAdapter implements RecommendationPort {

    private static final Logger LOG = LoggerFactory.getLogger(RecomendadorPorReglasAdapter.class);

    private final PlantillasExplicacion plantillas = new PlantillasExplicacion();

    @Override
    public MotorRecomendacion motor() {
        return MotorRecomendacion.REGLAS;
    }

    @Override
    public List<Recomendacion> recomendar(Preferencias preferencias, List<OfertaEvaluada> candidatas) {
        long inicio = System.nanoTime();
        List<Recomendacion> recomendaciones = new ArrayList<>();
        for (OfertaEvaluada evaluada : candidatas.subList(0, Math.min(ResultadoRecomendacion.MAXIMO, candidatas.size()))) {
            Explicacion explicacion = new Explicacion(List.copyOf(evaluada.criteriosCumplidos()),
                    plantillas.explicar(evaluada, preferencias));
            recomendaciones.add(new Recomendacion(recomendaciones.size() + 1, evaluada.oferta(), explicacion));
        }
        LOG.info("motor=REGLAS recomendaciones={} latenciaMs={}", recomendaciones.size(),
                (System.nanoTime() - inicio) / 1_000_000);
        return List.copyOf(recomendaciones);
    }
}
