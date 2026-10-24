package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.OfertaRecomendadaResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionRequest;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionesResponse;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Convierte la consulta en preferencias del dominio, que validan sus reglas, y el resultado en la respuesta.
 */
public final class RecomendacionWebMapper {

    private RecomendacionWebMapper() {
    }

    public static Preferencias aPreferencias(RecomendacionRequest consulta) {
        return new Preferencias(consulta.texto(), Idioma.deCodigo(consulta.idioma()),
                conjunto(consulta.tiposOferta(), TipoOferta.class),
                conjunto(consulta.tiposActividad(), TipoActividad.class), consulta.zonaId(),
                Boolean.TRUE.equals(consulta.accesibleSillaRuedas()), consulta.nivelEsfuerzoMaximo(),
                consulta.precioMaximo());
    }

    public static RecomendacionesResponse aRespuesta(ResultadoRecomendacion resultado) {
        return new RecomendacionesResponse(resultado.motor(), resultado.aviso(),
                resultado.recomendaciones().stream().map(RecomendacionWebMapper::aRespuesta).toList());
    }

    private static RecomendacionResponse aRespuesta(Recomendacion recomendacion) {
        OfertaCandidata oferta = recomendacion.oferta();
        return new RecomendacionResponse(recomendacion.posicion(),
                new OfertaRecomendadaResponse(oferta.id(), oferta.nombre(), oferta.tipo(), oferta.zonaNombre(),
                        oferta.precioReferencia(), oferta.unidadPrecio()),
                recomendacion.explicacion().criterios(), recomendacion.explicacion().texto());
    }

    /** Un null dentro de la lista, como {@code [null]} en el JSON, se ignora. */
    private static <E extends Enum<E>> Set<E> conjunto(List<E> valores, Class<E> tipo) {
        Set<E> conjunto = EnumSet.noneOf(tipo);
        if (valores != null) {
            valores.stream().filter(Objects::nonNull).forEach(conjunto::add);
        }
        return conjunto;
    }
}
