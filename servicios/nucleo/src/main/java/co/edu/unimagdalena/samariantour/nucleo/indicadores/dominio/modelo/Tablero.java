package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica.ReglaAnonimizacion;

import java.util.Comparator;
import java.util.List;

/**
 * La demanda del destino por zona y tipo de oferta en un periodo (RF-85, RF-86). Primero las combinaciones con más
 * solicitudes; las que no se publican van al final, por zona, para que el orden no diga si son 1 o 2 (RET-03).
 *
 * @param total vacío si es 1 o 2
 */
public record Tablero(Periodo periodo, Long total, List<DemandaZona> filas) {

    public static Tablero de(Periodo periodo, List<ConteoDemanda> conteos) {
        Comparator<ConteoDemanda> orden = Comparator.comparing(Tablero::oculto)
                .thenComparingLong(conteo -> oculto(conteo) ? 0 : -conteo.solicitudes())
                .thenComparing(ConteoDemanda::zonaId)
                .thenComparing(ConteoDemanda::tipoOferta);
        List<DemandaZona> filas = conteos.stream()
                .filter(conteo -> conteo.solicitudes() > 0)
                .sorted(orden)
                .map(conteo -> new DemandaZona(conteo.zonaId(), conteo.tipoOferta(),
                        ReglaAnonimizacion.publicable(conteo.solicitudes())))
                .toList();
        long total = conteos.stream().mapToLong(ConteoDemanda::solicitudes).sum();
        return new Tablero(periodo, ReglaAnonimizacion.publicable(total), filas);
    }

    private static boolean oculto(ConteoDemanda conteo) {
        return ReglaAnonimizacion.publicable(conteo.solicitudes()) == null;
    }
}
