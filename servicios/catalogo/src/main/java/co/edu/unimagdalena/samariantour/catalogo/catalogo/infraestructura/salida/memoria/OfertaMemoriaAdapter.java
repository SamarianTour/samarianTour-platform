package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

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
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Ofertas en la memoria del proceso, para el perfil {@code memoria}: la demostración y las pruebas sin MongoDB.
 *
 * <p>Guarda y entrega copias, igual que una base de datos: un cambio que no se guarda no queda registrado.
 * Cada réplica del servicio tiene su propia memoria, así que lo que se escribe en una no lo ve la otra.
 *
 * <p>Simula el bloqueo optimista de MongoDB (ADR-05): guarda solo si la revisión no cambió desde la lectura,
 * y cada guardado o actualización del RNT sube la revisión.
 */
@Component
@Profile("memoria")
public class OfertaMemoriaAdapter implements OfertaRepositoryPort {

    private final Map<String, Oferta> ofertas = new ConcurrentHashMap<>();

    /** Arranca con los datos de demostración de Santa Marta. */
    public OfertaMemoriaAdapter() {
        this(DatosSantaMarta.ofertas());
    }

    /** Guarda las iniciales como cualquier oferta, así que quedan con la revisión siguiente, como en MongoDB. */
    public OfertaMemoriaAdapter(Collection<Oferta> iniciales) {
        iniciales.forEach(this::guardarComparandoRevision);
    }

    /** Mismo formato que en MongoDB, para que los identificadores no cambien al cambiar de perfil (DC-30). */
    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public Optional<Oferta> buscarPorId(String id) {
        return Optional.ofNullable(ofertas.get(id)).map(CopiaOferta::de);
    }

    @Override
    public void guardar(Oferta oferta) {
        guardarComparandoRevision(oferta);
    }

    private void guardarComparandoRevision(Oferta oferta) {
        // compute es atómico por clave: la comparación y el guardado no se intercalan con otra escritura.
        ofertas.compute(oferta.id(), (id, guardada) -> {
            long revisionGuardada = guardada == null ? Oferta.REVISION_NUEVA : guardada.revision();
            if (revisionGuardada != oferta.revision()) {
                throw new ConflictoConcurrenciaException("La oferta %s cambió desde que se leyó".formatted(id));
            }
            return CopiaOferta.de(oferta, oferta.rntPrestadorActivo(), oferta.versionHabilitacion(),
                    oferta.revision() + 1);
        });
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
                .sorted(Comparator.comparing(OfertaMemoriaAdapter::precio).thenComparing(Oferta::id))
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
                ? CopiaOferta.de(oferta, rntActivo, version, oferta.revision() + 1)
                : oferta);
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
        return ReglasPaquete.actividadIdsDe(oferta).stream()
                .map(ofertas::get)
                .filter(actividad -> actividad != null)
                .toList();
    }

    private static VersionOferta publicada(Oferta oferta) {
        return oferta.publicada().orElseThrow();
    }

    private static BigDecimal precio(Oferta oferta) {
        return publicada(oferta).contenido().precioReferencia();
    }

    private static Pagina<Oferta> paginar(List<Oferta> todas, Paginacion paginacion) {
        int desde = (int) Math.min(paginacion.desplazamiento(), todas.size());
        int hasta = Math.min(desde + paginacion.tamano(), todas.size());
        List<Oferta> pagina = todas.subList(desde, hasta).stream().map(CopiaOferta::de).toList();
        return new Pagina<>(pagina, paginacion, todas.size());
    }
}
