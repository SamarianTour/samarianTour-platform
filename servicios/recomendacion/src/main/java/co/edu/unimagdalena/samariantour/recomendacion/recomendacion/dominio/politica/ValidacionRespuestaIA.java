package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RespuestaIAInvalidaException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.PropuestaIA;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Decide si se acepta la propuesta del proveedor de IA (DR-05). Se acepta completa o se descarta completa, y
 * entonces responde el motor por reglas: nada parcial.
 * <ul>
 *   <li>Entre 3 y 5 ofertas, o todas si la lista corta tenía menos de 3.</li>
 *   <li>Ids únicos y todos de la lista corta que se le envió.</li>
 *   <li>Al menos un criterio por oferta; cada uno, del enum y cierto para esa oferta y esas preferencias.</li>
 *   <li>Explicación no vacía y de 300 caracteres o menos.</li>
 * </ul>
 */
public class ValidacionRespuestaIA {

    private final ReglaCriteriosExplicitos regla;

    public ValidacionRespuestaIA(ReglaCriteriosExplicitos regla) {
        this.regla = regla;
    }

    /**
     * @throws RespuestaIAInvalidaException con el motivo, que va al log; nunca lleva el texto del turista
     */
    public List<Recomendacion> validar(PropuestaIA propuesta, List<OfertaEvaluada> listaCorta,
                                       Preferencias preferencias) {
        if (propuesta == null || propuesta.recomendaciones() == null) {
            throw new RespuestaIAInvalidaException("la respuesta no trae la lista de recomendaciones");
        }
        List<PropuestaIA.Item> items = propuesta.recomendaciones();
        int minimo = Math.min(ResultadoRecomendacion.MINIMO, listaCorta.size());
        int maximo = Math.min(ResultadoRecomendacion.MAXIMO, listaCorta.size());
        if (items.size() < minimo || items.size() > maximo) {
            throw new RespuestaIAInvalidaException(
                    "propone %d ofertas; se esperaban entre %d y %d".formatted(items.size(), minimo, maximo));
        }
        Map<String, OfertaCandidata> permitidas = listaCorta.stream().map(OfertaEvaluada::oferta)
                .collect(Collectors.toMap(OfertaCandidata::id, Function.identity()));
        Set<String> vistas = new HashSet<>();
        List<Recomendacion> recomendaciones = new ArrayList<>();
        for (PropuestaIA.Item item : items) {
            OfertaCandidata oferta = item == null ? null : permitidas.get(item.ofertaId());
            if (oferta == null) {
                throw new RespuestaIAInvalidaException("propone una oferta que no está en la lista");
            }
            if (!vistas.add(oferta.id())) {
                throw new RespuestaIAInvalidaException("propone dos veces la oferta " + oferta.id());
            }
            List<CriterioAfinidad> criterios = criterios(item.criterios(), oferta, preferencias);
            recomendaciones.add(new Recomendacion(recomendaciones.size() + 1, oferta,
                    new Explicacion(criterios, explicacion(item.explicacion(), oferta))));
        }
        return List.copyOf(recomendaciones);
    }

    private List<CriterioAfinidad> criterios(List<String> textos, OfertaCandidata oferta, Preferencias preferencias) {
        if (textos == null || textos.isEmpty()) {
            throw new RespuestaIAInvalidaException("la oferta " + oferta.id() + " no cita ningún criterio");
        }
        Set<CriterioAfinidad> criterios = new LinkedHashSet<>();
        for (String texto : textos) {
            CriterioAfinidad criterio = criterio(texto);
            if (!regla.esCierto(criterio, oferta, preferencias)) {
                throw new RespuestaIAInvalidaException(
                        "el criterio %s no es cierto para la oferta %s".formatted(criterio, oferta.id()));
            }
            criterios.add(criterio);
        }
        return List.copyOf(criterios);
    }

    private static CriterioAfinidad criterio(String texto) {
        try {
            return CriterioAfinidad.valueOf(texto == null ? "" : texto.strip());
        } catch (IllegalArgumentException e) {
            throw new RespuestaIAInvalidaException("cita un criterio que no existe");
        }
    }

    private static String explicacion(String texto, OfertaCandidata oferta) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty() || limpio.length() > Explicacion.TEXTO_MAXIMO) {
            throw new RespuestaIAInvalidaException("la explicación de la oferta %s está vacía o pasa de %d caracteres"
                    .formatted(oferta.id(), Explicacion.TEXTO_MAXIMO));
        }
        return limpio;
    }
}
