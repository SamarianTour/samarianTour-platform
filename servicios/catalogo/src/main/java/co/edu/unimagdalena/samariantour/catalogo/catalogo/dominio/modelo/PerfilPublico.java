package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que el turista ve del prestador en la ficha de sus ofertas (RF-24, RF-64). No incluye datos de contacto directo.
 * Se asocia al prestador por su identificador, y el prestador lo gestiona; los cambios se publican sin moderación
 * (DC-39).
 *
 * <p>Solo la descripción es obligatoria, para no dejar fuera a los prestadores pequeños con pocos recursos
 * tecnológicos (DC-26). El sitio web y las redes sociales, si vienen, son URLs http o https.
 *
 * <p>Es inmutable: cada cambio produce un perfil nuevo con la misma revisión, y la persistencia la sube al guardar.
 *
 * @param nombreComercial  nombre con el que el prestador se presenta al turista; opcional
 * @param fotos            fotos que el prestador subió, en el orden en que las subió; como máximo {@link #MAXIMO_FOTOS}
 * @param horariosAtencion horario general de atención del prestador
 * @param redesSociales    enlaces a redes sociales
 * @param revision         revisión técnica con la que se leyó, para el bloqueo optimista (ADR-05); ninguna regla de
 *                         negocio la interpreta
 */
public record PerfilPublico(
        UUID prestadorId,
        String nombreComercial,
        String descripcion,
        List<Foto> fotos,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales,
        long revision) {

    public static final int MAXIMO_FOTOS = 6;

    /** Revisión de un perfil que todavía no se ha guardado. */
    public static final long REVISION_NUEVA = 0;

    public PerfilPublico {
        Objects.requireNonNull(prestadorId, "prestadorId");
        nombreComercial = Validaciones.textoOpcional(nombreComercial);
        descripcion = Validaciones.texto(descripcion, "descripcion");
        fotos = Validaciones.lista(fotos, "fotos");
        if (fotos.size() > MAXIMO_FOTOS) {
            throw new DatoInvalidoException("fotos", "un perfil tiene como máximo %d fotos".formatted(MAXIMO_FOTOS));
        }
        if (new HashSet<>(fotos).size() != fotos.size()) {
            throw new IllegalArgumentException("Una foto no puede estar dos veces en el perfil");
        }
        horariosAtencion = Validaciones.textoOpcional(horariosAtencion);
        sitioWeb = Validaciones.urlOpcional(sitioWeb, "sitioWeb");
        redesSociales = Validaciones.urls(redesSociales, "redesSociales");
    }

    /** Un perfil que todavía no se ha guardado. */
    public PerfilPublico(UUID prestadorId, String nombreComercial, String descripcion, List<Foto> fotos,
                         String horariosAtencion, String sitioWeb, List<String> redesSociales) {
        this(prestadorId, nombreComercial, descripcion, fotos, horariosAtencion, sitioWeb, redesSociales,
                REVISION_NUEVA);
    }

    /** El mismo perfil con otros datos; las fotos no cambian, porque se agregan y se quitan de a una. */
    public PerfilPublico conDatos(String nombreComercial, String descripcion, String horariosAtencion,
                                  String sitioWeb, List<String> redesSociales) {
        return new PerfilPublico(prestadorId, nombreComercial, descripcion, fotos, horariosAtencion, sitioWeb,
                redesSociales, revision);
    }

    /**
     * El mismo perfil con la foto al final.
     *
     * @throws DatoInvalidoException si ya tiene {@link #MAXIMO_FOTOS} fotos
     */
    public PerfilPublico agregarFoto(Foto foto) {
        verificarCupoDeFotos();
        List<Foto> nuevas = new ArrayList<>(fotos);
        nuevas.add(Objects.requireNonNull(foto, "foto"));
        return conFotos(nuevas);
    }

    /** El mismo perfil sin esa foto. */
    public PerfilPublico quitarFoto(Foto foto) {
        return conFotos(fotos.stream().filter(actual -> !actual.equals(foto)).toList());
    }

    public boolean tieneFoto(Foto foto) {
        return fotos.contains(foto);
    }

    /** @throws DatoInvalidoException si el perfil ya tiene {@link #MAXIMO_FOTOS} fotos */
    public void verificarCupoDeFotos() {
        if (fotos.size() >= MAXIMO_FOTOS) {
            throw new DatoInvalidoException("fotos", "un perfil tiene como máximo %d fotos".formatted(MAXIMO_FOTOS));
        }
    }

    private PerfilPublico conFotos(List<Foto> nuevas) {
        return new PerfilPublico(prestadorId, nombreComercial, descripcion, nuevas, horariosAtencion, sitioWeb,
                redesSociales, revision);
    }
}
