package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Una versión de la oferta, subdocumento {@code publicada} o {@code pendiente} (DC-16). El tipo está en la raíz de la
 * oferta y cada versión solo lleva los campos de su tipo: los demás quedan en null y no se guardan.
 *
 * <p>Los horarios de salida son textos {@code HH:mm} y la ubicación es un punto GeoJSON {@code [longitud, latitud]}.
 */
public record VersionDocument(
        // Moderación (DC-05)
        int numero,
        EstadoModeracion estadoModeracion,
        String motivoRechazo,
        Instant enviadaEn,
        Instant revisadaEn,

        // Contenido común
        String nombre,
        String descripcion,
        // Sin Decimal128, Spring lo guardaría como texto y el filtro por rango de precio fallaría (DC-16).
        @Field(targetType = FieldType.DECIMAL128) BigDecimal precioReferencia,
        String horarios,
        ObjectId zonaId,
        GeoJsonPoint ubicacion,
        String comoLlegar,
        int capacidadMaxima,

        // ACTIVIDAD
        TipoActividad tipoActividad,
        NivelEsfuerzo nivelEsfuerzo,
        String puntoEncuentro,
        ModalidadRecorrido modalidad,
        List<String> idiomasAtencion,

        // ACTIVIDAD y PAQUETE
        List<String> horariosSalida,
        Integer duracionMinutos,

        // PAQUETE
        List<ObjectId> actividadIds,

        // ALOJAMIENTO
        String tipoUnidad,

        // ACTIVIDAD, ALOJAMIENTO y RESTAURANTE
        Boolean accesibleSillaRuedas) {

    public static final String ZONA_ID = "zonaId";
    public static final String PRECIO_REFERENCIA = "precioReferencia";
    public static final String ACTIVIDAD_IDS = "actividadIds";
    public static final String TIPO_ACTIVIDAD = "tipoActividad";
    public static final String NIVEL_ESFUERZO = "nivelEsfuerzo";
    public static final String ACCESIBLE_SILLA_RUEDAS = "accesibleSillaRuedas";
}
