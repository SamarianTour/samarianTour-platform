package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.FotoNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FotoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilPublicoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Convierte el perfil público y sus fotos (RF-24, RF-64, DC-39).
 */
public final class PerfilWebMapper {

    private PerfilWebMapper() {
    }

    public static GuardarPerfilCommand aComando(UUID prestadorId, PerfilRequest peticion) {
        return new GuardarPerfilCommand(prestadorId, peticion.nombreComercial(), peticion.descripcion(),
                peticion.horariosAtencion(), peticion.sitioWeb(), peticion.redesSociales());
    }

    public static PerfilPublicoResponse aRespuesta(PerfilPublico perfil) {
        return new PerfilPublicoResponse(perfil.nombreComercial(), perfil.descripcion(),
                perfil.fotos().stream().map(PerfilWebMapper::aFoto).toList(), perfil.horariosAtencion(),
                perfil.sitioWeb(), perfil.redesSociales());
    }

    /** El tamaño que nombra la ruta de una foto; uno desconocido responde como una foto que no existe. */
    public static TamanoFoto aTamano(String fotoId, String tamano) {
        return Arrays.stream(TamanoFoto.values())
                .filter(valor -> segmento(valor).equals(tamano))
                .findFirst()
                .orElseThrow(() -> new FotoNoEncontradaException(fotoId));
    }

    private static FotoResponse aFoto(Foto foto) {
        return new FotoResponse(foto.id(), ruta(foto, TamanoFoto.GALERIA), ruta(foto, TamanoFoto.MINIATURA));
    }

    /** Ruta de la foto dentro del catálogo, con su prefijo {@code /api/catalogo} y sin el dominio. */
    private static String ruta(Foto foto, TamanoFoto tamano) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/fotos/{id}/{tamano}")
                .buildAndExpand(foto.id(), segmento(tamano))
                .getPath();
    }

    private static String segmento(TamanoFoto tamano) {
        return tamano.name().toLowerCase(Locale.ROOT);
    }
}
