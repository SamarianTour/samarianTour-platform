package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.FotoNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FotoResponse;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Convierte las fotos de perfiles y ofertas (DC-39, DC-47). El dominio guarda solo el identificador; aquí se arma la
 * ruta de cada tamaño.
 */
public final class FotoWebMapper {

    private FotoWebMapper() {
    }

    public static FotoResponse aRespuesta(Foto foto) {
        return new FotoResponse(foto.id(), ruta(foto, TamanoFoto.GALERIA), ruta(foto, TamanoFoto.MINIATURA));
    }

    public static List<FotoResponse> aRespuestas(List<Foto> fotos) {
        return fotos.stream().map(FotoWebMapper::aRespuesta).toList();
    }

    /** La primera foto, que es la portada de la oferta; null si la versión es anterior a DC-47 y no tiene. */
    public static FotoResponse portada(List<Foto> fotos) {
        return fotos.isEmpty() ? null : aRespuesta(fotos.getFirst());
    }

    /**
     * Las fotos que el prestador pone en una oferta, por su identificador. Si falta la lista, queda vacía y el dominio
     * responde que hace falta al menos una.
     *
     * @throws DatoInvalidoException en {@code fotos} si un identificador no es de foto
     */
    public static List<Foto> aFotos(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(FotoWebMapper::aFoto).toList();
    }

    /** El tamaño que nombra la ruta de una foto; uno desconocido responde como una foto que no existe. */
    public static TamanoFoto aTamano(String fotoId, String tamano) {
        return Arrays.stream(TamanoFoto.values())
                .filter(valor -> segmento(valor).equals(tamano))
                .findFirst()
                .orElseThrow(() -> new FotoNoEncontradaException(fotoId));
    }

    private static Foto aFoto(String id) {
        try {
            return new Foto(id);
        } catch (DatoInvalidoException e) {
            throw new DatoInvalidoException("fotos", "tiene un identificador que no es de una foto");
        }
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
