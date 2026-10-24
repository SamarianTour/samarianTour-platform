package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Los criterios explícitos con los que se elige una oferta (RF-27, restricción ética del enunciado, DR-03). La
 * aplican los dos motores antes de recomendar.
 * <ol>
 *   <li>Filtros duros: si el turista pide accesibilidad, solo ofertas accesibles; precio dentro del máximo; esfuerzo
 *   dentro del máximo (las ofertas sin nivel, como alojamientos y restaurantes, pasan).</li>
 *   <li>Afinidad: un punto por cada criterio blando que coincide (tipo de oferta, algún tipo de actividad, zona).</li>
 * </ol>
 * Nunca usa calificaciones, opiniones, comisión, tamaño ni identidad del prestador.
 */
public class ReglaCriteriosExplicitos {

    /** Las que pasan los filtros duros, en el mismo orden en que llegaron. */
    public List<OfertaEvaluada> evaluar(List<OfertaCandidata> ofertas, Preferencias preferencias) {
        return ofertas.stream()
                .filter(oferta -> pasaFiltrosDuros(oferta, preferencias))
                .map(oferta -> {
                    Set<CriterioAfinidad> cumplidos = criteriosCumplidos(oferta, preferencias);
                    int afinidad = (int) cumplidos.stream().filter(CriterioAfinidad::esBlando).count();
                    return new OfertaEvaluada(oferta, cumplidos, afinidad);
                })
                .toList();
    }

    public boolean pasaFiltrosDuros(OfertaCandidata oferta, Preferencias preferencias) {
        if (preferencias.accesibleSillaRuedas() && !oferta.accesibleSillaRuedas()) {
            return false;
        }
        if (preferencias.precioMaximo() != null && !dentroDelPrecio(oferta, preferencias)) {
            return false;
        }
        return preferencias.nivelEsfuerzoMaximo() == null || oferta.nivelEsfuerzo() == null
                || oferta.nivelEsfuerzo().noSupera(preferencias.nivelEsfuerzoMaximo());
    }

    /**
     * Los blandos que coinciden y los duros que el turista pidió y la oferta cumple. El esfuerzo solo cuenta si la
     * oferta tiene nivel: pasar el filtro por no tenerlo no es una razón para recomendarla.
     */
    public Set<CriterioAfinidad> criteriosCumplidos(OfertaCandidata oferta, Preferencias preferencias) {
        Set<CriterioAfinidad> cumplidos = EnumSet.noneOf(CriterioAfinidad.class);
        if (preferencias.tiposOferta().contains(oferta.tipo())) {
            cumplidos.add(CriterioAfinidad.TIPO_OFERTA);
        }
        if (!Collections.disjoint(preferencias.tiposActividad(), oferta.tiposActividad())) {
            cumplidos.add(CriterioAfinidad.TIPO_ACTIVIDAD);
        }
        if (preferencias.zonaId() != null && preferencias.zonaId().equals(oferta.zonaId())) {
            cumplidos.add(CriterioAfinidad.ZONA);
        }
        if (preferencias.accesibleSillaRuedas() && oferta.accesibleSillaRuedas()) {
            cumplidos.add(CriterioAfinidad.ACCESIBILIDAD);
        }
        if (dentroDelEsfuerzo(oferta, preferencias)) {
            cumplidos.add(CriterioAfinidad.NIVEL_ESFUERZO);
        }
        if (preferencias.precioMaximo() != null && dentroDelPrecio(oferta, preferencias)) {
            cumplidos.add(CriterioAfinidad.PRECIO);
        }
        return cumplidos;
    }

    /**
     * Si el criterio que cita el proveedor de IA es cierto para esa oferta y esas preferencias (DR-05).
     * <p>Un criterio blando que el turista no eligió en los filtros solo se acepta si escribió un texto: la IA lo
     * puede haber deducido de sus palabras, y la oferta sí tiene ese dato. Los duros se comprueban siempre contra la
     * oferta: si dice accesible, lo es; si dice precio o esfuerzo, el turista dio el máximo y la oferta lo cumple.
     */
    public boolean esCierto(CriterioAfinidad criterio, OfertaCandidata oferta, Preferencias preferencias) {
        return switch (criterio) {
            case TIPO_OFERTA -> preferencias.tiposOferta().isEmpty()
                    ? preferencias.tieneTexto()
                    : preferencias.tiposOferta().contains(oferta.tipo());
            case TIPO_ACTIVIDAD -> preferencias.tiposActividad().isEmpty()
                    ? preferencias.tieneTexto() && !oferta.tiposActividad().isEmpty()
                    : !Collections.disjoint(preferencias.tiposActividad(), oferta.tiposActividad());
            case ZONA -> preferencias.zonaId() == null
                    ? preferencias.tieneTexto()
                    : preferencias.zonaId().equals(oferta.zonaId());
            case ACCESIBILIDAD -> oferta.accesibleSillaRuedas();
            case NIVEL_ESFUERZO -> dentroDelEsfuerzo(oferta, preferencias);
            case PRECIO -> preferencias.precioMaximo() != null && dentroDelPrecio(oferta, preferencias);
        };
    }

    private static boolean dentroDelPrecio(OfertaCandidata oferta, Preferencias preferencias) {
        return oferta.precioReferencia().compareTo(preferencias.precioMaximo()) <= 0;
    }

    private static boolean dentroDelEsfuerzo(OfertaCandidata oferta, Preferencias preferencias) {
        return preferencias.nivelEsfuerzoMaximo() != null && oferta.nivelEsfuerzo() != null
                && oferta.nivelEsfuerzo().noSupera(preferencias.nivelEsfuerzoMaximo());
    }
}
