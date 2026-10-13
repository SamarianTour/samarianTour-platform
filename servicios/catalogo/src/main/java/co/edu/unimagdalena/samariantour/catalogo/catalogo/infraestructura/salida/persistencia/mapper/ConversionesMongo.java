package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Conversiones de tipos entre el dominio y los documentos.
 */
public final class ConversionesMongo {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private ConversionesMongo() {
    }

    /**
     * El identificador como ObjectId, o vacío si no tiene ese formato: un id así no puede existir en la base, y quien
     * lo busca lo trata como inexistente (DC-30).
     */
    public static Optional<ObjectId> objectId(String id) {
        return id != null && ObjectId.isValid(id) ? Optional.of(new ObjectId(id)) : Optional.empty();
    }

    /** Los identificadores con formato ObjectId; omite los demás. */
    public static List<ObjectId> objectIds(Collection<String> ids) {
        return ids.stream().map(ConversionesMongo::objectId).flatMap(Optional::stream).toList();
    }

    /**
     * Una referencia que el dominio ya validó (la zona o las actividades de un paquete existen), así que siempre
     * tiene formato ObjectId.
     */
    static ObjectId referencia(String id) {
        return objectId(id).orElseThrow(() ->
                new IllegalArgumentException("La referencia %s no tiene formato ObjectId (DC-30)".formatted(id)));
    }

    /** GeoJSON guarda primero la longitud y después la latitud. */
    static GeoJsonPoint punto(Coordenada coordenada) {
        return coordenada == null ? null : new GeoJsonPoint(coordenada.longitud(), coordenada.latitud());
    }

    static Coordenada coordenada(GeoJsonPoint punto) {
        return punto == null ? null : new Coordenada(punto.getY(), punto.getX());
    }

    static List<String> horas(List<LocalTime> horas) {
        return horas.stream().map(HORA::format).toList();
    }

    static List<LocalTime> aHoras(List<String> horas) {
        return horas == null ? List.of() : horas.stream().map(hora -> LocalTime.parse(hora, HORA)).toList();
    }

    static <T> List<T> lista(List<T> valores) {
        return valores == null ? List.of() : valores;
    }
}
