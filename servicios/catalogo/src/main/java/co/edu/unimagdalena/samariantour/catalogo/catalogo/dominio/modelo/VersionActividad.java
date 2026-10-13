package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Versión de una oferta de tipo actividad.
 * Punto de encuentro, duración, modalidad e idiomas son opcionales aquí; para el guía independiente
 * los exige ReglaTipoOferta (RF-67).
 */
public final class VersionActividad extends VersionOferta {

    private static final Pattern CODIGO_IDIOMA = Pattern.compile("[a-z]{2}");

    private final TipoActividad tipoActividad;
    private final NivelEsfuerzo nivelEsfuerzo;
    private final boolean accesibleSillaRuedas;
    private final List<LocalTime> horariosSalida;
    private final String puntoEncuentro;
    private final Integer duracionMinutos;
    private final ModalidadRecorrido modalidad;
    private final List<String> idiomasAtencion;

    /** Crea un borrador, aún sin enviar a moderación. */
    public VersionActividad(ContenidoComun contenido, TipoActividad tipoActividad, NivelEsfuerzo nivelEsfuerzo,
                            boolean accesibleSillaRuedas, List<LocalTime> horariosSalida, String puntoEncuentro,
                            Integer duracionMinutos, ModalidadRecorrido modalidad, List<String> idiomasAtencion) {
        this(contenido, tipoActividad, nivelEsfuerzo, accesibleSillaRuedas, horariosSalida, puntoEncuentro,
                duracionMinutos, modalidad, idiomasAtencion, null);
    }

    private VersionActividad(ContenidoComun contenido, TipoActividad tipoActividad, NivelEsfuerzo nivelEsfuerzo,
                             boolean accesibleSillaRuedas, List<LocalTime> horariosSalida, String puntoEncuentro,
                             Integer duracionMinutos, ModalidadRecorrido modalidad, List<String> idiomasAtencion,
                             RegistroModeracion moderacion) {
        super(contenido, moderacion);
        this.tipoActividad = Validaciones.presente(tipoActividad, "tipoActividad");
        this.nivelEsfuerzo = Validaciones.presente(nivelEsfuerzo, "nivelEsfuerzo");
        this.accesibleSillaRuedas = accesibleSillaRuedas;
        // El turista elige una hora de salida al enviar la solicitud (RF-30): se necesita al menos una.
        this.horariosSalida = Validaciones.listaNoVacia(horariosSalida, "horariosSalida");
        this.puntoEncuentro = Validaciones.textoOpcional(puntoEncuentro);
        this.duracionMinutos = Validaciones.positivoOpcional(duracionMinutos, "duracionMinutos");
        this.modalidad = modalidad;
        this.idiomasAtencion = validarIdiomas(idiomasAtencion);
    }

    /** Reconstruye una versión ya enviada; solo para los adaptadores de persistencia. */
    public static VersionActividad reconstituir(ContenidoComun contenido, TipoActividad tipoActividad,
                                                NivelEsfuerzo nivelEsfuerzo, boolean accesibleSillaRuedas,
                                                List<LocalTime> horariosSalida, String puntoEncuentro,
                                                Integer duracionMinutos, ModalidadRecorrido modalidad,
                                                List<String> idiomasAtencion, RegistroModeracion moderacion) {
        return new VersionActividad(contenido, tipoActividad, nivelEsfuerzo, accesibleSillaRuedas, horariosSalida,
                puntoEncuentro, duracionMinutos, modalidad, idiomasAtencion,
                Validaciones.presente(moderacion, "moderacion"));
    }

    private static List<String> validarIdiomas(List<String> idiomas) {
        List<String> copia = Validaciones.lista(idiomas, "idiomasAtencion");
        if (copia.stream().anyMatch(idioma -> !CODIGO_IDIOMA.matcher(idioma).matches())) {
            throw new DatoInvalidoException("idiomasAtencion", "usa códigos de dos letras en minúscula, como \"es\" o \"en\"");
        }
        return copia;
    }

    @Override
    VersionActividad conModeracion(RegistroModeracion moderacion) {
        return new VersionActividad(contenido(), tipoActividad, nivelEsfuerzo, accesibleSillaRuedas, horariosSalida,
                puntoEncuentro, duracionMinutos, modalidad, idiomasAtencion, moderacion);
    }

    @Override
    public TipoOferta tipo() {
        return TipoOferta.ACTIVIDAD;
    }

    @Override
    public String unidadPrecio() {
        return "por persona";
    }

    public boolean esAccesibleSillaRuedas() {
        return accesibleSillaRuedas;
    }

    public TipoActividad tipoActividad() {
        return tipoActividad;
    }

    public NivelEsfuerzo nivelEsfuerzo() {
        return nivelEsfuerzo;
    }

    public List<LocalTime> horariosSalida() {
        return horariosSalida;
    }

    public String puntoEncuentro() {
        return puntoEncuentro;
    }

    public Integer duracionMinutos() {
        return duracionMinutos;
    }

    public ModalidadRecorrido modalidad() {
        return modalidad;
    }

    public List<String> idiomasAtencion() {
        return idiomasAtencion;
    }
}
