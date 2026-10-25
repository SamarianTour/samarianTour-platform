package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Qué ofertas se le mostraron a un turista, en qué posición y desde qué consulta (RET-02, DR-08, DR-20). Sirve para
 * comprobar que ningún prestador sale favorecido: no lleva el texto ni ningún dato del turista.
 */
public record RegistroExposicion(Instant fecha, OrigenExposicion origen, MotorRecomendacion motor,
                                 List<OfertaExpuesta> ofertas) {

    public RegistroExposicion {
        ofertas = List.copyOf(ofertas);
    }

    public record OfertaExpuesta(String ofertaId, UUID prestadorId, int posicion, List<CriterioAfinidad> criterios) {

        public OfertaExpuesta {
            criterios = List.copyOf(criterios);
        }
    }

    public static RegistroExposicion de(Instant fecha, ResultadoRecomendacion resultado) {
        return new RegistroExposicion(fecha, OrigenExposicion.RECOMENDACION, resultado.motor(),
                expuestas(resultado.recomendaciones()));
    }

    /** Las alternativas siempre salen del motor por reglas: no usan la IA (DR-19). */
    public static RegistroExposicion de(Instant fecha, ResultadoAlternativas resultado) {
        return new RegistroExposicion(fecha, OrigenExposicion.ALTERNATIVAS, MotorRecomendacion.REGLAS,
                expuestas(resultado.alternativas()));
    }

    private static List<OfertaExpuesta> expuestas(List<Recomendacion> recomendaciones) {
        return recomendaciones.stream()
                .map(r -> new OfertaExpuesta(r.oferta().id(), r.oferta().prestadorId(), r.posicion(),
                        r.explicacion().criterios()))
                .toList();
    }
}
