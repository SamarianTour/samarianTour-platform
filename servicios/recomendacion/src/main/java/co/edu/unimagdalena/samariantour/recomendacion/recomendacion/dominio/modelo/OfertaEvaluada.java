package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.List;
import java.util.Set;

/**
 * Una oferta que pasó los filtros duros, con su afinidad, los criterios que cumple y las palabras del texto del
 * turista que coinciden con ella (DR-03, DR-13).
 *
 * @param criteriosCumplidos los blandos que coinciden y los duros que el turista pidió y la oferta cumple
 * @param afinidad           un punto por cada criterio blando que coincide
 * @param palabrasDelTexto   las palabras del texto que coinciden con la oferta, como las escribió el turista
 */
public record OfertaEvaluada(OfertaCandidata oferta, Set<CriterioAfinidad> criteriosCumplidos, int afinidad,
                             List<String> palabrasDelTexto) {

    public OfertaEvaluada {
        criteriosCumplidos = Preferencias.copia(criteriosCumplidos, CriterioAfinidad.class);
        palabrasDelTexto = List.copyOf(palabrasDelTexto);
    }

    /** Sin texto del turista. */
    public OfertaEvaluada(OfertaCandidata oferta, Set<CriterioAfinidad> criteriosCumplidos, int afinidad) {
        this(oferta, criteriosCumplidos, afinidad, List.of());
    }

    /** Cuántas palabras del texto coinciden: desempata entre ofertas de igual afinidad. */
    public int relevancia() {
        return palabrasDelTexto.size();
    }
}
