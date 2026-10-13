package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Doble en memoria de OfertaRepositoryPort con la misma semántica que el adaptador de MongoDB: guarda y entrega
 * copias, y aplica el bloqueo optimista por revisión (ADR-05). Cuenta las veces que se guarda cada oferta para
 * detectar un servicio que olvide guardar, y puede simular conflictos de concurrencia.
 */
class OfertaRepositoryEnMemoria implements OfertaRepositoryPort {

    private final Map<String, Oferta> ofertas = new LinkedHashMap<>();
    private int secuenciaIds;
    private final Map<String, Integer> guardadas = new HashMap<>();
    private int conflictosPorSimular;

    /** Ids con formato ObjectId (24 dígitos hexadecimales), predecibles para las pruebas. */
    @Override
    public String nuevoId() {
        return "%024x".formatted(++secuenciaIds);
    }

    @Override
    public Optional<Oferta> buscarPorId(String id) {
        return Optional.ofNullable(ofertas.get(id)).map(CopiaEnPrueba::de);
    }

    @Override
    public void guardar(Oferta oferta) {
        if (conflictosPorSimular > 0) {
            conflictosPorSimular--;
            throw new ConflictoConcurrenciaException("Conflicto simulado en la prueba");
        }
        Oferta guardada = ofertas.get(oferta.id());
        long revisionGuardada = guardada == null ? Oferta.REVISION_NUEVA : guardada.revision();
        if (revisionGuardada != oferta.revision()) {
            throw new ConflictoConcurrenciaException("La oferta " + oferta.id() + " cambió desde que se leyó");
        }
        ofertas.put(oferta.id(), CopiaEnPrueba.de(oferta, oferta.rntPrestadorActivo(), oferta.versionHabilitacion(),
                oferta.revision() + 1));
        guardadas.merge(oferta.id(), 1, Integer::sum);
    }

    /** Los próximos {@code cantidad} guardados fallan con un conflicto, como si otra réplica escribiera antes. */
    void simularConflictos(int cantidad) {
        conflictosPorSimular = cantidad;
    }

    @Override
    public Pagina<Oferta> buscarVisibles(CriteriosBusqueda criterios) {
        Predicate<Oferta> cumple = this::esVisible;
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
        if (criterios.tipoActividad() != null) {
            cumple = cumple.and(oferta -> tiposActividad(oferta).contains(criterios.tipoActividad()));
        }
        if (criterios.nivelEsfuerzoMaximo() != null) {
            cumple = cumple.and(oferta -> {
                NivelEsfuerzo nivel = nivelEsfuerzo(oferta);
                return nivel != null && nivel.noSupera(criterios.nivelEsfuerzoMaximo());
            });
        }
        if (criterios.soloAccesibleSillaRuedas()) {
            cumple = cumple.and(this::esAccesibleSillaRuedas);
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
    public void actualizarRntPrestador(UUID prestadorId, boolean rntActivo, long version) {
        ofertas.replaceAll((id, oferta) -> oferta.esDe(prestadorId) && oferta.versionHabilitacion() < version
                ? CopiaEnPrueba.de(oferta, rntActivo, version, oferta.revision() + 1)
                : oferta);
    }

    int vecesGuardada(String id) {
        return guardadas.getOrDefault(id, 0);
    }

    int total() {
        return ofertas.size();
    }

    /** Un paquete además exige que todas sus actividades sean visibles (RF-22). */
    private boolean esVisible(Oferta oferta) {
        return ReglasPaquete.esVisible(oferta, actividadesDe(oferta));
    }

    /** Tipos de actividad de una oferta visible: el propio en una actividad y los de sus actividades en un paquete. */
    private Set<TipoActividad> tiposActividad(Oferta oferta) {
        return switch (publicada(oferta)) {
            case VersionActividad actividad -> Set.of(actividad.tipoActividad());
            case VersionPaquete paquete -> ReglasPaquete.tiposActividad(actividadesDe(oferta));
            case VersionAlojamiento alojamiento -> Set.of();
            case VersionRestaurante restaurante -> Set.of();
        };
    }

    /** Nivel de esfuerzo de una oferta visible; alojamientos y restaurantes no tienen. */
    private NivelEsfuerzo nivelEsfuerzo(Oferta oferta) {
        return switch (publicada(oferta)) {
            case VersionActividad actividad -> actividad.nivelEsfuerzo();
            case VersionPaquete paquete -> ReglasPaquete.nivelEsfuerzo(actividadesDe(oferta));
            case VersionAlojamiento alojamiento -> null;
            case VersionRestaurante restaurante -> null;
        };
    }

    private boolean esAccesibleSillaRuedas(Oferta oferta) {
        return switch (publicada(oferta)) {
            case VersionActividad actividad -> actividad.esAccesibleSillaRuedas();
            case VersionPaquete paquete -> ReglasPaquete.esAccesibleSillaRuedas(actividadesDe(oferta));
            case VersionAlojamiento alojamiento -> alojamiento.esAccesibleSillaRuedas();
            case VersionRestaurante restaurante -> restaurante.esAccesibleSillaRuedas();
        };
    }

    private List<Oferta> actividadesDe(Oferta oferta) {
        return buscarPorIds(ReglasPaquete.actividadIdsDe(oferta));
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
