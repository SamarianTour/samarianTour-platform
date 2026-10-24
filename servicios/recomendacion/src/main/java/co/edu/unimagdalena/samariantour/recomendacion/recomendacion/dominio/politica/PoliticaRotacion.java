package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Rota el orden entre ofertas de igual afinidad (RF-27): en cada consulta las empata al azar, para que ningún
 * prestador quede siempre primero por el orden en que el catálogo las entrega (precio e id). Antes del azar desempatan
 * las palabras del texto del turista que coinciden con cada oferta (DR-13). La fuente de azar se inyecta para que las
 * pruebas sean deterministas.
 */
public class PoliticaRotacion {

    private final RandomGenerator azar;

    public PoliticaRotacion(RandomGenerator azar) {
        this.azar = Objects.requireNonNull(azar, "azar");
    }

    /**
     * De mayor a menor afinidad; con la misma afinidad, de más a menos palabras del texto que coinciden; con las dos
     * iguales, en un orden al azar.
     */
    public List<OfertaEvaluada> ordenar(List<OfertaEvaluada> evaluadas) {
        List<OfertaEvaluada> ordenadas = new ArrayList<>(barajar(evaluadas));
        // El ordenamiento es estable: conserva el orden al azar entre las empatadas.
        ordenadas.sort(Comparator.comparingInt(OfertaEvaluada::afinidad)
                .thenComparingInt(OfertaEvaluada::relevancia).reversed());
        return List.copyOf(ordenadas);
    }

    /** Una copia en orden al azar (Fisher-Yates). */
    public <T> List<T> barajar(List<T> elementos) {
        List<T> copia = new ArrayList<>(elementos);
        for (int i = copia.size() - 1; i > 0; i--) {
            Collections.swap(copia, i, azar.nextInt(i + 1));
        }
        return copia;
    }
}
