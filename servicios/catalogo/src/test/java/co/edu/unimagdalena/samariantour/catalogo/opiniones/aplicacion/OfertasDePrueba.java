package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Doble del catálogo para las pruebas de los servicios: ofertas que existen, con su nombre, y cuáles son visibles.
 */
class OfertasDePrueba implements OfertaCatalogoPort {

    private final Map<String, String> nombres = new HashMap<>();
    private final Set<String> visibles = new HashSet<>();
    private int consultasDeNombres;

    OfertasDePrueba visible(String ofertaId, String nombre) {
        nombres.put(ofertaId, nombre);
        visibles.add(ofertaId);
        return this;
    }

    /** Existe, pero el turista no la ve: retirada, sin aprobar o de un prestador sin RNT. */
    OfertasDePrueba noVisible(String ofertaId, String nombre) {
        nombres.put(ofertaId, nombre);
        visibles.remove(ofertaId);
        return this;
    }

    int consultasDeNombres() {
        return consultasDeNombres;
    }

    @Override
    public boolean existe(String ofertaId) {
        return nombres.containsKey(ofertaId);
    }

    @Override
    public boolean esVisible(String ofertaId) {
        return visibles.contains(ofertaId);
    }

    @Override
    public Map<String, String> nombres(Collection<String> ofertaIds) {
        consultasDeNombres++;
        return ofertaIds.stream().filter(nombres::containsKey).distinct()
                .collect(Collectors.toMap(id -> id, nombres::get));
    }
}
