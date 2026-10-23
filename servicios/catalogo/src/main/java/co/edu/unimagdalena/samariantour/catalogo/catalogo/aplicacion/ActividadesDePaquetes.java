package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carga las actividades de los paquetes, que el paquete referencia por identificador (RF-22). Las necesitan las
 * reglas de visibilidad, esfuerzo, tipos de actividad y accesibilidad del paquete (ReglasPaquete).
 */
final class ActividadesDePaquetes {

    private final OfertaRepositoryPort ofertas;

    ActividadesDePaquetes(OfertaRepositoryPort ofertas) {
        this.ofertas = ofertas;
    }

    /** Actividades de un paquete, en el orden en que las incluye; vacía para los demás tipos de oferta. */
    List<Oferta> de(Oferta oferta) {
        return de(List.of(oferta)).get(oferta.id());
    }

    /**
     * Actividades de cada oferta, por id de la oferta, con una sola consulta para todas. Cada lista va en el orden
     * en que el paquete incluye sus actividades y omite las que ya no existen; es vacía si la oferta no es un paquete.
     */
    Map<String, List<Oferta>> de(Collection<Oferta> lote) {
        List<String> ids = lote.stream()
                .flatMap(oferta -> ReglasPaquete.actividadIdsDe(oferta).stream())
                .distinct()
                .toList();
        Map<String, Oferta> cargadas = ids.isEmpty() ? Map.of() : ofertas.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Oferta::id, Function.identity(), (primera, repetida) -> primera));
        return lote.stream().collect(Collectors.toMap(Oferta::id,
                oferta -> ReglasPaquete.actividadIdsDe(oferta).stream()
                        .map(cargadas::get)
                        .filter(Objects::nonNull)
                        .toList(),
                (primera, repetida) -> primera));
    }
}
