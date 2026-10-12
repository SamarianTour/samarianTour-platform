package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Doble en memoria de OfertaRepositoryPort con la misma semántica que el adaptador de MongoDB.
 * Cuenta las veces que se guarda cada oferta para detectar un servicio que olvide guardar.
 */
class OfertaRepositoryEnMemoria implements OfertaRepositoryPort {

    private final Map<String, Oferta> ofertas = new LinkedHashMap<>();
    private final Map<String, Integer> guardadas = new HashMap<>();

    @Override
    public Optional<Oferta> buscarPorId(String id) {
        return Optional.ofNullable(ofertas.get(id));
    }

    @Override
    public void guardar(Oferta oferta) {
        ofertas.put(oferta.id(), oferta);
        guardadas.merge(oferta.id(), 1, Integer::sum);
    }

    @Override
    public Pagina<Oferta> buscarVisibles(CriteriosBusqueda criterios) {
        Predicate<Oferta> cumple = Oferta::estaVisible;
        if (criterios.tipo() != null) {
            cumple = cumple.and(oferta -> oferta.tipo() == criterios.tipo());
        }
        if (criterios.zonaId() != null) {
            cumple = cumple.and(oferta -> publicada(oferta).contenido().zonaId().equals(criterios.zonaId()));
        }
        if (criterios.precioMin() != null) {
            cumple = cumple.and(oferta -> precio(oferta).compareTo(criterios.precioMin()) >= 0);
        }
        if (criterios.precioMax() != null) {
            cumple = cumple.and(oferta -> precio(oferta).compareTo(criterios.precioMax()) <= 0);
        }
        List<Oferta> resultado = ofertas.values().stream()
                .filter(cumple)
                .sorted(Comparator.comparing(OfertaRepositoryEnMemoria::precio).thenComparing(Oferta::id))
                .toList();
        return paginar(resultado, criterios.paginacion());
    }

    @Override
    public Pagina<Oferta> buscarPorPrestador(UUID prestadorId, Paginacion paginacion) {
        List<Oferta> resultado = ofertas.values().stream()
                .filter(oferta -> oferta.esDe(prestadorId))
                .sorted(Comparator.comparing(Oferta::id))
                .toList();
        return paginar(resultado, paginacion);
    }

    @Override
    public void actualizarRntPrestador(UUID prestadorId, boolean rntActivo) {
        ofertas.replaceAll((id, oferta) -> oferta.esDe(prestadorId)
                ? Oferta.reconstituir(oferta.id(), oferta.tipo(), oferta.prestadorId(), oferta.retirada(), rntActivo,
                        oferta.publicada().orElse(null), oferta.pendiente().orElse(null))
                : oferta);
    }

    int vecesGuardada(String id) {
        return guardadas.getOrDefault(id, 0);
    }

    int total() {
        return ofertas.size();
    }

    private static VersionOferta publicada(Oferta oferta) {
        return oferta.publicada().orElseThrow();
    }

    private static BigDecimal precio(Oferta oferta) {
        return publicada(oferta).contenido().precioReferencia();
    }

    private static <T> Pagina<T> paginar(List<T> todos, Paginacion paginacion) {
        int desde = (int) Math.min(paginacion.desplazamiento(), todos.size());
        int hasta = Math.min(desde + paginacion.tamano(), todos.size());
        return new Pagina<>(todos.subList(desde, hasta), paginacion, todos.size());
    }
}
