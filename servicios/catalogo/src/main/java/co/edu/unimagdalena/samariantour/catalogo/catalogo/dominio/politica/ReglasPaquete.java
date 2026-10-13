package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reglas de los paquetes (RF-17, RF-22): un paquete reúne dos o más actividades del mismo prestador; toma el mayor
 * nivel de esfuerzo de sus actividades; es accesible en silla de ruedas solo si todas lo son; y es visible solo si
 * todas sus actividades son visibles.
 *
 * <p>El paquete referencia sus actividades por identificador, así que estas reglas reciben las ofertas ya cargadas:
 * las busca quien las invoca (la capa de aplicación o el adaptador de búsqueda).
 */
public final class ReglasPaquete {

    public static final int MINIMO_ACTIVIDADES = 2;

    private ReglasPaquete() {
    }

    /**
     * Verifica, al publicar o editar, que cada oferta que el paquete referencia sea una actividad del mismo
     * prestador (RF-22) y que no esté retirada (DC-23). Una referencia que no está entre las ofertas recibidas
     * se trata como inexistente. Una actividad aún sin aprobar sí se admite: el prestador puede enviar a moderación
     * las actividades y el paquete al mismo tiempo, y el paquete no es visible hasta que todas lo sean.
     */
    public static void verificarActividades(UUID prestadorId, VersionPaquete paquete, Collection<Oferta> actividades) {
        Map<String, Oferta> porId = porId(actividades);
        for (String actividadId : paquete.actividadIds()) {
            Oferta actividad = porId.get(actividadId);
            if (actividad == null || actividad.tipo() != TipoOferta.ACTIVIDAD || !actividad.esDe(prestadorId)) {
                throw new DatoInvalidoException("actividadIds",
                        "%s no es una actividad del prestador (RF-22)".formatted(actividadId));
            }
            if (actividad.retirada()) {
                throw new DatoInvalidoException("actividadIds",
                        "%s está retirada y no se puede incluir en un paquete".formatted(actividadId));
            }
        }
    }

    /**
     * Un paquete es visible si cumple ReglaVisibilidad y además todas las actividades de su versión publicada
     * son visibles (RF-22). Para una oferta que no es un paquete equivale a ReglaVisibilidad.
     *
     * @param actividades ofertas de {@link #actividadIdsDe}; una que falte se trata como no visible
     */
    public static boolean esVisible(Oferta paquete, Collection<Oferta> actividades) {
        if (!ReglaVisibilidad.esVisible(paquete)) {
            return false;
        }
        Map<String, Oferta> porId = porId(actividades);
        return actividadIdsDe(paquete).stream()
                .allMatch(id -> porId.containsKey(id) && ReglaVisibilidad.esVisible(porId.get(id)));
    }

    /**
     * Nivel de esfuerzo del paquete: el mayor entre sus actividades (RF-17).
     *
     * @param actividades las actividades del paquete, todas con versión publicada; lo cumple un paquete visible
     */
    public static NivelEsfuerzo nivelEsfuerzo(Collection<Oferta> actividades) {
        return versionesPublicadas(actividades)
                .map(VersionActividad::nivelEsfuerzo)
                .reduce(NivelEsfuerzo::mayor)
                .orElseThrow(() -> new IllegalArgumentException("Un paquete tiene al menos una actividad"));
    }

    /**
     * Tipos de actividad del paquete: los de sus actividades. El filtro por tipo de actividad incluye un paquete
     * si alguno coincide (DC-24).
     *
     * @param actividades las actividades del paquete, todas con versión publicada; lo cumple un paquete visible
     */
    public static Set<TipoActividad> tiposActividad(Collection<Oferta> actividades) {
        return versionesPublicadas(actividades)
                .map(VersionActividad::tipoActividad)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Un paquete es accesible en silla de ruedas solo si todas sus actividades lo son (RF-17).
     *
     * @param actividades las actividades del paquete, todas con versión publicada; lo cumple un paquete visible
     */
    public static boolean esAccesibleSillaRuedas(Collection<Oferta> actividades) {
        return versionesPublicadas(actividades).allMatch(VersionActividad::esAccesibleSillaRuedas);
    }

    /** Actividades de la versión publicada del paquete; vacío si no tiene versión publicada o no es un paquete. */
    public static List<String> actividadIdsDe(Oferta paquete) {
        return paquete.publicada()
                .filter(VersionPaquete.class::isInstance)
                .map(version -> ((VersionPaquete) version).actividadIds())
                .orElse(List.of());
    }

    private static Stream<VersionActividad> versionesPublicadas(Collection<Oferta> actividades) {
        return actividades.stream().map(actividad -> actividad.publicada()
                .filter(VersionActividad.class::isInstance)
                .map(VersionActividad.class::cast)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La oferta %s no es una actividad publicada".formatted(actividad.id()))));
    }

    private static Map<String, Oferta> porId(Collection<Oferta> ofertas) {
        return ofertas.stream()
                .collect(Collectors.toMap(Oferta::id, Function.identity(), (primera, repetida) -> primera));
    }
}
