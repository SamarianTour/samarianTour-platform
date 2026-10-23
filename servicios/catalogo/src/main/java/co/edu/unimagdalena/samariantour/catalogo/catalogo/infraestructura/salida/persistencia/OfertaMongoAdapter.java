package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.VersionDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.OfertaMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.PENDIENTE;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.PRESTADOR_ID;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.RETIRADA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.REVISION;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.RNT_PRESTADOR_ACTIVO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.VERSION_HABILITACION;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Ofertas en la colección {@code ofertas} de MongoDB (DC-16).
 *
 * <p>Bloqueo optimista del ADR-05, igual que el adaptador en memoria: una oferta nueva (revisión 0) se inserta con
 * la revisión 1, y una oferta leída se reemplaza solo si la revisión guardada sigue siendo la que se leyó, y queda
 * con la siguiente. La comparación y el reemplazo son una sola operación atómica de MongoDB.
 */
@Component
@Profile("mongo")
public class OfertaMongoAdapter implements OfertaRepositoryPort {

    private final MongoTemplate mongo;

    public OfertaMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public Optional<Oferta> buscarPorId(String id) {
        return ConversionesMongo.objectId(id)
                .map(objectId -> mongo.findById(objectId, OfertaDocument.class))
                .map(OfertaMongoMapper::aDominio);
    }

    /** Una sola consulta con {@code $in}; omite los ids que no existen o que no tienen formato ObjectId. */
    @Override
    public List<Oferta> buscarPorIds(Collection<String> ids) {
        return mongo.find(query(where("_id").in(ConversionesMongo.objectIds(ids))), OfertaDocument.class).stream()
                .map(OfertaMongoMapper::aDominio)
                .toList();
    }

    @Override
    public void guardar(Oferta oferta) {
        if (oferta.revision() == Oferta.REVISION_NUEVA) {
            insertar(oferta);
            return;
        }
        Query leida = query(where("_id").is(ConversionesMongo.objectId(oferta.id()).orElseThrow())
                .and(REVISION).is(oferta.revision()));
        long reemplazadas = mongo.replace(leida, OfertaMongoMapper.aDocumento(oferta, oferta.revision() + 1))
                .getMatchedCount();
        if (reemplazadas == 0) {
            throw conflicto(oferta);
        }
    }

    @Override
    public Pagina<Oferta> buscarVisibles(CriteriosBusqueda criterios) {
        ObjectId zonaId = null;
        if (criterios.zonaId() != null) {
            Optional<ObjectId> zona = ConversionesMongo.objectId(criterios.zonaId());
            if (zona.isEmpty()) {
                return new Pagina<>(List.of(), criterios.paginacion(), 0);
            }
            zonaId = zona.get();
        }
        Document resultado = mongo.getCollection(OfertaDocument.COLECCION)
                .aggregate(BusquedaOfertasMongo.etapas(criterios, zonaId))
                .first();
        List<Oferta> contenido = resultado.getList(BusquedaOfertasMongo.CONTENIDO, Document.class).stream()
                .map(documento -> OfertaMongoMapper.aDominio(mongo.getConverter().read(OfertaDocument.class, documento)))
                .toList();
        List<Document> total = resultado.getList(BusquedaOfertasMongo.TOTAL, Document.class);
        long totalElementos = total.isEmpty() ? 0 : total.getFirst().get(BusquedaOfertasMongo.TOTAL, Number.class)
                .longValue();
        return new Pagina<>(contenido, criterios.paginacion(), totalElementos);
    }

    /** Usa el índice {@code {prestadorId, _id}}, que también da el orden. */
    @Override
    public Pagina<Oferta> buscarPorPrestador(UUID prestadorId, Paginacion paginacion) {
        Query delPrestador = query(where(PRESTADOR_ID).is(prestadorId));
        long total = mongo.count(delPrestador, OfertaDocument.class);
        List<Oferta> contenido = mongo.find(Query.of(delPrestador)
                        .with(Sort.by("_id"))
                        .skip(paginacion.desplazamiento())
                        .limit(paginacion.tamano()), OfertaDocument.class).stream()
                .map(OfertaMongoMapper::aDominio)
                .toList();
        return new Pagina<>(contenido, paginacion, total);
    }

    /** Lo resuelve el índice parcial {@code en_moderacion}, que solo guarda estas ofertas (DC-38). */
    @Override
    public Pagina<Oferta> buscarEnModeracion(Paginacion paginacion) {
        Query pendientes = new BasicQuery(enModeracion());
        long total = mongo.count(pendientes, OfertaDocument.class);
        List<Oferta> contenido = mongo.find(Query.of(pendientes)
                        .with(Sort.by(PENDIENTE_ENVIADA_EN, "_id"))
                        .skip(paginacion.desplazamiento())
                        .limit(paginacion.tamano()), OfertaDocument.class).stream()
                .map(OfertaMongoMapper::aDominio)
                .toList();
        return new Pagina<>(contenido, paginacion, total);
    }

    /** Fecha de envío de la versión pendiente: ordena las ofertas en moderación (DC-38). */
    static final String PENDIENTE_ENVIADA_EN = PENDIENTE + "." + VersionDocument.ENVIADA_EN;

    /**
     * Condiciones de Oferta.estaEnModeracion. Son las del filtro parcial del índice {@code en_moderacion}: MongoDB
     * solo usa un índice parcial si la consulta las incluye.
     */
    static Document enModeracion() {
        return new Document(RETIRADA, false)
                .append(PENDIENTE + "." + VersionDocument.ESTADO_MODERACION, EstadoModeracion.EN_MODERACION.name());
    }

    /**
     * Un {@code updateMany} filtrado por {@code versionHabilitacion}: un aviso atrasado no encuentra ofertas que
     * cumplan el filtro y no cambia nada. Sube la revisión para que una edición o una aprobación cruzada se reintente
     * con el RNT ya actualizado (ADR-05).
     */
    @Override
    public void actualizarRntPrestador(UUID prestadorId, boolean rntActivo, long version) {
        mongo.updateMulti(query(where(PRESTADOR_ID).is(prestadorId).and(VERSION_HABILITACION).lt(version)),
                new Update().set(RNT_PRESTADOR_ACTIVO, rntActivo).set(VERSION_HABILITACION, version).inc(REVISION, 1),
                OfertaDocument.class);
    }

    private void insertar(Oferta oferta) {
        try {
            mongo.insert(OfertaMongoMapper.aDocumento(oferta, Oferta.REVISION_NUEVA + 1));
        } catch (DuplicateKeyException e) {
            throw conflicto(oferta);
        }
    }

    private static ConflictoConcurrenciaException conflicto(Oferta oferta) {
        return new ConflictoConcurrenciaException("La oferta %s cambió desde que se leyó".formatted(oferta.id()));
    }
}
