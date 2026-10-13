package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.VersionDocument;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.PUBLICADA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.RETIRADA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.RNT_PRESTADOR_ACTIVO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.TIPO;

/**
 * La búsqueda de ofertas visibles como agregación (DC-31). Aplica las mismas reglas que el adaptador en memoria:
 * ReglaVisibilidad, la visibilidad de un paquete y sus valores calculados con sus actividades (ReglasPaquete, DC-24,
 * DC-25).
 *
 * <ol>
 *   <li>{@code $match} con las condiciones del filtro parcial de los índices de QAS-07 (DC-17), el tipo, la zona
 *       y el rango de precio: lo resuelve un índice.</li>
 *   <li>{@code $sort} por precio y luego por id, el orden estable de DC-14. Va antes del {@code $lookup} para que
 *       también lo resuelva el índice; las etapas siguientes conservan el orden.</li>
 *   <li>Si el resultado puede incluir paquetes, {@code $lookup} trae las actividades visibles de cada paquete, y un
 *       paquete solo queda si todas sus actividades son visibles (RF-22).</li>
 *   <li>{@code $match} con los filtros que dependen de las actividades del paquete: tipo de actividad, nivel de
 *       esfuerzo y accesibilidad.</li>
 *   <li>{@code $facet} entrega la página pedida y el total en una sola consulta.</li>
 * </ol>
 */
final class BusquedaOfertasMongo {

    /** Campo temporal con las actividades visibles de un paquete; no se guarda. */
    private static final String ACTIVIDADES = "actividades";
    static final String CONTENIDO = "contenido";
    static final String TOTAL = "total";

    private static final String TIPO_ACTIVIDAD = PUBLICADA + "." + VersionDocument.TIPO_ACTIVIDAD;
    private static final String NIVEL_ESFUERZO = PUBLICADA + "." + VersionDocument.NIVEL_ESFUERZO;
    private static final String ACCESIBLE = PUBLICADA + "." + VersionDocument.ACCESIBLE_SILLA_RUEDAS;
    private static final String ACTIVIDAD_IDS = PUBLICADA + "." + VersionDocument.ACTIVIDAD_IDS;
    static final String ZONA_ID = PUBLICADA + "." + VersionDocument.ZONA_ID;
    static final String PRECIO = PUBLICADA + "." + VersionDocument.PRECIO_REFERENCIA;

    private BusquedaOfertasMongo() {
    }

    /**
     * Condiciones de ReglaVisibilidad. Son las mismas del filtro parcial de los índices de búsqueda: MongoDB solo
     * usa un índice parcial si la consulta las incluye.
     */
    static Document visibles() {
        return new Document(RNT_PRESTADOR_ACTIVO, true)
                .append(RETIRADA, false)
                .append(PUBLICADA, new Document("$exists", true));
    }

    /**
     * Etapas de la agregación.
     *
     * @param zonaId la zona de los criterios ya convertida; null si no se filtra por zona
     */
    static List<Document> etapas(CriteriosBusqueda criterios, ObjectId zonaId) {
        List<Document> etapas = new ArrayList<>();
        etapas.add(new Document("$match", filtroIndexado(criterios, zonaId)));
        etapas.add(new Document("$sort", new Document(PRECIO, 1).append("_id", 1)));
        if (puedeIncluirPaquetes(criterios)) {
            etapas.add(actividadesVisibles());
            etapas.add(new Document("$match", paqueteConTodasSusActividadesVisibles()));
        }
        List<Document> filtros = filtrosPorActividades(criterios);
        if (!filtros.isEmpty()) {
            etapas.add(new Document("$match", new Document("$and", filtros)));
        }
        etapas.add(new Document("$facet", new Document(CONTENIDO, List.of(
                new Document("$skip", criterios.paginacion().desplazamiento()),
                new Document("$limit", criterios.paginacion().tamano())))
                .append(TOTAL, List.of(new Document("$count", TOTAL)))));
        return etapas;
    }

    private static Document filtroIndexado(CriteriosBusqueda criterios, ObjectId zonaId) {
        Document filtro = visibles();
        if (criterios.tipo() != null) {
            filtro.append(TIPO, criterios.tipo().name());
        }
        if (zonaId != null) {
            filtro.append(ZONA_ID, zonaId);
        }
        Document precio = new Document();
        if (criterios.precioMin() != null) {
            precio.append("$gte", new Decimal128(criterios.precioMin()));
        }
        if (criterios.precioMax() != null) {
            precio.append("$lte", new Decimal128(criterios.precioMax()));
        }
        if (!precio.isEmpty()) {
            filtro.append(PRECIO, precio);
        }
        return filtro;
    }

    private static boolean puedeIncluirPaquetes(CriteriosBusqueda criterios) {
        return criterios.tipo() == null || criterios.tipo() == TipoOferta.PAQUETE;
    }

    /**
     * Las actividades visibles del paquete, solo con los campos que necesitan los filtros. En una oferta que no es
     * un paquete no hay {@code actividadIds} y el resultado queda vacío.
     */
    private static Document actividadesVisibles() {
        return new Document("$lookup", new Document("from", OfertaDocument.COLECCION)
                .append("localField", ACTIVIDAD_IDS)
                .append("foreignField", "_id")
                .append("pipeline", List.of(
                        new Document("$match", visibles()),
                        new Document("$project", new Document("_id", 0)
                                .append(TIPO_ACTIVIDAD, 1)
                                .append(NIVEL_ESFUERZO, 1)
                                .append(ACCESIBLE, 1))))
                .append("as", ACTIVIDADES));
    }

    /** Un paquete es visible si encontró todas sus actividades entre las visibles (RF-22). */
    private static Document paqueteConTodasSusActividadesVisibles() {
        return new Document("$or", List.of(
                new Document(TIPO, new Document("$ne", TipoOferta.PAQUETE.name())),
                new Document("$expr", new Document("$eq", List.of(
                        new Document("$size", "$" + ACTIVIDADES),
                        new Document("$size", new Document("$ifNull", List.of("$" + ACTIVIDAD_IDS, List.of()))))))));
    }

    private static List<Document> filtrosPorActividades(CriteriosBusqueda criterios) {
        List<Document> filtros = new ArrayList<>();
        if (criterios.tipoActividad() != null) {
            filtros.add(porTipoActividad(criterios.tipoActividad().name()));
        }
        if (criterios.nivelEsfuerzoMaximo() != null) {
            filtros.add(hastaElNivel(criterios.nivelEsfuerzoMaximo()));
        }
        if (criterios.soloAccesibleSillaRuedas()) {
            filtros.add(soloAccesibles());
        }
        return filtros;
    }

    /** Una actividad de ese tipo, o un paquete con al menos una actividad de ese tipo (DC-24). */
    private static Document porTipoActividad(String tipoActividad) {
        return new Document("$or", List.of(
                new Document(TIPO, TipoOferta.ACTIVIDAD.name()).append(TIPO_ACTIVIDAD, tipoActividad),
                new Document(TIPO, TipoOferta.PAQUETE.name()).append(ACTIVIDADES + "." + TIPO_ACTIVIDAD,
                        tipoActividad)));
    }

    /**
     * Una actividad con un nivel que no supera el máximo, o un paquete sin ninguna actividad que lo supere: su nivel
     * es el mayor de sus actividades (DC-25). Los niveles permitidos los calcula NivelEsfuerzo, que define el orden.
     * Alojamientos y restaurantes quedan fuera.
     */
    private static Document hastaElNivel(NivelEsfuerzo maximo) {
        List<String> permitidos = Arrays.stream(NivelEsfuerzo.values())
                .filter(nivel -> nivel.noSupera(maximo))
                .map(Enum::name)
                .toList();
        return new Document("$or", List.of(
                new Document(TIPO, TipoOferta.ACTIVIDAD.name())
                        .append(NIVEL_ESFUERZO, new Document("$in", permitidos)),
                new Document(TIPO, TipoOferta.PAQUETE.name())
                        .append(ACTIVIDADES, new Document("$not", new Document("$elemMatch",
                                new Document(NIVEL_ESFUERZO, new Document("$nin", permitidos)))))));
    }

    /** Una oferta accesible, o un paquete cuyas actividades lo son todas (RF-17). */
    private static Document soloAccesibles() {
        return new Document("$or", List.of(
                new Document(TIPO, new Document("$ne", TipoOferta.PAQUETE.name())).append(ACCESIBLE, true),
                new Document(TIPO, TipoOferta.PAQUETE.name())
                        .append(ACTIVIDADES, new Document("$not", new Document("$elemMatch",
                                new Document(ACCESIBLE, new Document("$ne", true)))))));
    }
}
