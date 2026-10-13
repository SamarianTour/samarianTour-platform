package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que el turista ve del prestador en la ficha de sus ofertas (RF-24, RF-64). No incluye datos de contacto directo.
 * Se asocia al prestador por su identificador.
 *
 * <p>Solo la descripción es obligatoria, para no dejar fuera a los prestadores pequeños con pocos recursos
 * tecnológicos (DC-26). Las fotos, el sitio web y las redes sociales, si vienen, son URLs http o https.
 *
 * @param nombreComercial  nombre con el que el prestador se presenta al turista; opcional
 * @param fotos            URLs de las fotos
 * @param horariosAtencion horario general de atención del prestador
 * @param redesSociales    enlaces a redes sociales
 */
// TODO: definir si hay un límite de cantidad de fotos; hoy no lo hay.
public record PerfilPublico(
        UUID prestadorId,
        String nombreComercial,
        String descripcion,
        List<String> fotos,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales) {

    public PerfilPublico {
        Objects.requireNonNull(prestadorId, "prestadorId");
        nombreComercial = Validaciones.textoOpcional(nombreComercial);
        descripcion = Validaciones.texto(descripcion, "descripcion");
        fotos = Validaciones.urls(fotos, "fotos");
        horariosAtencion = Validaciones.textoOpcional(horariosAtencion);
        sitioWeb = Validaciones.urlOpcional(sitioWeb, "sitioWeb");
        redesSociales = Validaciones.urls(redesSociales, "redesSociales");
    }
}
