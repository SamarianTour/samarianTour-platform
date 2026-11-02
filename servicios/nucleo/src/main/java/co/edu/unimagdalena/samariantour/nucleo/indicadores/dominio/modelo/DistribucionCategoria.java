package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica.ReglaAnonimizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cómo se reparte la demanda de un periodo entre las categorías de prestador (RF-89), con la categoría que tenía cada
 * prestador al enviarse la solicitud (DN-41). Siempre salen las cuatro, aunque tengan 0.
 *
 * @param total vacío si es 1 o 2
 */
public record DistribucionCategoria(Periodo periodo, Long total, List<ParticipacionCategoria> categorias) {

    public static final List<String> CATEGORIAS = List.of("GUIA_INDEPENDIENTE", "MICRO_PEQUENO", "MEDIANO", "GRANDE");
    /** Las solicitudes anteriores a la copia de la categoría cuyo prestador no se encontró (V14). */
    public static final String SIN_CATEGORIA = "SIN_CATEGORIA";

    public static DistribucionCategoria de(Periodo periodo, List<ConteoCategoria> conteos) {
        Map<String, ConteoCategoria> porCategoria = conteos.stream().collect(Collectors.toMap(
                conteo -> conteo.categoria() == null ? SIN_CATEGORIA : conteo.categoria(), Function.identity()));
        List<String> nombres = new ArrayList<>(CATEGORIAS);
        if (porCategoria.containsKey(SIN_CATEGORIA)) {
            nombres.add(SIN_CATEGORIA);
        }
        long total = conteos.stream().mapToLong(ConteoCategoria::solicitudes).sum();
        List<ParticipacionCategoria> categorias = nombres.stream()
                .map(nombre -> {
                    ConteoCategoria conteo = porCategoria.getOrDefault(nombre, new ConteoCategoria(nombre, 0, 0));
                    return new ParticipacionCategoria(nombre, ReglaAnonimizacion.publicable(conteo.solicitudes()),
                            ReglaAnonimizacion.porcentaje(conteo.solicitudes(), total),
                            ReglaAnonimizacion.publicable(conteo.pagadas()));
                })
                .toList();
        return new DistribucionCategoria(periodo, ReglaAnonimizacion.publicable(total), categorias);
    }
}
