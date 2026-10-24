package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Una zona de la colección {@code zonas} (RF-84, DC-18).
 *
 * @param nombreClave el nombre sin tildes, en minúsculas y con espacios simples (Zona.claveNombre); tiene un índice
 *                    único para que dos zonas no se llamen igual (DC-49)
 */
@Document(ZonaDocument.COLECCION)
public record ZonaDocument(
        @Id ObjectId id,
        String nombre,
        String nombreClave,
        String descripcion,
        GeoJsonPoint ubicacion,
        boolean areaProtegida,
        boolean altaAfluencia,
        boolean activa,
        List<String> buenasPracticas,
        String condicionesVisita) {

    public static final String COLECCION = "zonas";

    public static final String NOMBRE = "nombre";
    public static final String NOMBRE_CLAVE = "nombreClave";
    public static final String ACTIVA = "activa";
}
