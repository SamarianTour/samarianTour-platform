package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.List;

/**
 * Las alternativas a una oferta de una zona con alta afluencia (RF-29): de 2 a 3 ofertas del mismo tipo en zonas sin
 * alta afluencia, o de 0 a 1 con el aviso de pocas coincidencias.
 *
 * @param aplica     false si la zona de la oferta no es de alta afluencia: la ficha no muestra nada
 * @param aviso      null si no hay nada que advertir
 * @param zonaOrigen null si no aplica
 */
public record ResultadoAlternativas(boolean aplica, AvisoRecomendacion aviso, ZonaOrigen zonaOrigen,
                                    List<Recomendacion> alternativas) {

    public static final int MINIMO = 2;
    /** [SUPUESTO] (DR-18): RF-29 pide al menos dos; con tres la ficha no se recarga. */
    public static final int MAXIMO = 3;

    public ResultadoAlternativas {
        alternativas = List.copyOf(alternativas);
    }

    public static ResultadoAlternativas noAplica() {
        return new ResultadoAlternativas(false, null, null, List.of());
    }

    /** @param nombre null si el catálogo no lo pudo dar */
    public record ZonaOrigen(String id, String nombre) {
    }
}
