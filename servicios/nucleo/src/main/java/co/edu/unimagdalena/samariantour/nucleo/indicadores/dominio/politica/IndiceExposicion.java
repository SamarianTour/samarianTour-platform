package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AparicionesPrestador;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.MedicionExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertasElegibles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * El índice de exposición (RET-02, diagrama de paquetes): el porcentaje de las apariciones de un prestador en las
 * recomendaciones de los últimos 30 días dividido por su porcentaje de las ofertas elegibles. Si pasa de 1,5 con al
 * menos 200 recomendaciones, se alerta al administrador (DN-43).
 */
public final class IndiceExposicion {

    public static final Duration VENTANA = Duration.ofDays(30);
    public static final long MINIMO_RECOMENDACIONES = 200;
    public static final BigDecimal UMBRAL = new BigDecimal("1.5");

    private IndiceExposicion() {
    }

    /** Con menos de 200 recomendaciones cualquier diferencia puede ser casualidad: no se mide. */
    public static boolean hayDatosSuficientes(long recomendaciones) {
        return recomendaciones >= MINIMO_RECOMENDACIONES;
    }

    /**
     * Mide a cada prestador con ofertas elegibles. Uno que apareció y hoy no tiene ninguna visible, porque perdió el
     * RNT o las retiró, no se mide: no tiene oferta contra la cual comparar.
     */
    public static List<MedicionExposicion> medir(List<AparicionesPrestador> apariciones,
                                                 List<OfertasElegibles> elegibles) {
        long totalApariciones = apariciones.stream().mapToLong(AparicionesPrestador::apariciones).sum();
        long totalOfertas = elegibles.stream().mapToLong(OfertasElegibles::ofertas).sum();
        if (totalApariciones == 0 || totalOfertas == 0) {
            return List.of();
        }
        Map<UUID, Long> porPrestador = apariciones.stream().collect(Collectors.toMap(
                AparicionesPrestador::prestadorId, AparicionesPrestador::apariciones));
        return elegibles.stream()
                .filter(elegible -> elegible.ofertas() > 0)
                .map(elegible -> {
                    long aparece = porPrestador.getOrDefault(elegible.prestadorId(), 0L);
                    BigDecimal indice = BigDecimal.valueOf(aparece * totalOfertas)
                            .divide(BigDecimal.valueOf(elegible.ofertas() * totalApariciones), 2, RoundingMode.HALF_UP);
                    return new MedicionExposicion(elegible.prestadorId(), aparece,
                            porcentaje(aparece, totalApariciones), porcentaje(elegible.ofertas(), totalOfertas),
                            indice);
                })
                .toList();
    }

    public static boolean superaUmbral(MedicionExposicion medicion) {
        return medicion.indice().compareTo(UMBRAL) > 0;
    }

    private static BigDecimal porcentaje(long parte, long total) {
        return BigDecimal.valueOf(parte * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
