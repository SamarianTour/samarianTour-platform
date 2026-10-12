package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;

/**
 * Versión de una oferta de tipo paquete: dos o más actividades del mismo prestador (RF-22).
 * Las actividades se referencian por el identificador de su oferta. El nivel de esfuerzo y la accesibilidad
 * del paquete no se guardan: se calculan con sus actividades en ReglasPaquete (RF-17).
 */
public final class VersionPaquete extends VersionOferta {

    private final List<String> actividadIds;
    private final int duracionMinutos;
    private final List<LocalTime> horariosSalida;

    /** Crea un borrador, aún sin enviar a moderación. */
    public VersionPaquete(ContenidoComun contenido, List<String> actividadIds, int duracionMinutos,
                          List<LocalTime> horariosSalida) {
        this(contenido, actividadIds, duracionMinutos, horariosSalida, null);
    }

    private VersionPaquete(ContenidoComun contenido, List<String> actividadIds, int duracionMinutos,
                           List<LocalTime> horariosSalida, RegistroModeracion moderacion) {
        super(contenido, moderacion);
        this.actividadIds = validarActividades(actividadIds);
        this.duracionMinutos = Validaciones.positivo(duracionMinutos, "duracionMinutos");
        // El turista elige una hora de salida al enviar la solicitud (RF-30): se necesita al menos una.
        this.horariosSalida = Validaciones.listaNoVacia(horariosSalida, "horariosSalida");
    }

    /** Reconstruye una versión ya enviada; solo para los adaptadores de persistencia. */
    public static VersionPaquete reconstituir(ContenidoComun contenido, List<String> actividadIds,
                                              int duracionMinutos, List<LocalTime> horariosSalida,
                                              RegistroModeracion moderacion) {
        return new VersionPaquete(contenido, actividadIds, duracionMinutos, horariosSalida,
                Validaciones.presente(moderacion, "moderacion"));
    }

    private static List<String> validarActividades(List<String> actividadIds) {
        List<String> ids = Validaciones.lista(actividadIds, "actividadIds").stream()
                .map(id -> Validaciones.texto(id, "actividadIds"))
                .toList();
        if (ids.size() < ReglasPaquete.MINIMO_ACTIVIDADES) {
            throw new DatoInvalidoException("actividadIds",
                    "un paquete incluye al menos %d actividades (RF-22)".formatted(ReglasPaquete.MINIMO_ACTIVIDADES));
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new DatoInvalidoException("actividadIds", "no admite actividades repetidas");
        }
        return ids;
    }

    @Override
    public TipoOferta tipo() {
        return TipoOferta.PAQUETE;
    }

    /** Por persona, como las actividades que lo componen (DC-22). */
    @Override
    public String unidadPrecio() {
        return "por persona";
    }

    /** Identificadores de las ofertas de tipo actividad que incluye el paquete. */
    public List<String> actividadIds() {
        return actividadIds;
    }

    public int duracionMinutos() {
        return duracionMinutos;
    }

    public List<LocalTime> horariosSalida() {
        return horariosSalida;
    }
}
