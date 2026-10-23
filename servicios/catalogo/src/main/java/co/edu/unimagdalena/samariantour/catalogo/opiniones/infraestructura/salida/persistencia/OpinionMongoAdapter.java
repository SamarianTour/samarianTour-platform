package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.ResumenCalificaciones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.mapper.OpinionMongoMapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.CALIFICACION;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.ESTADO;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.FECHA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.OFERTA_ID;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.REVISION;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.TURISTA_ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Opiniones en la colección {@code opiniones} de MongoDB (DC-46).
 *
 * <p>Una opinión nueva se inserta con la revisión 1; el índice único {@code {ofertaId, turistaId}} rechaza la segunda
 * opinión del mismo turista sobre la misma oferta aunque llegue a otra réplica (RF-61). Una opinión leída se reemplaza
 * solo si su revisión no cambió (ADR-05), en una sola operación atómica.
 *
 * <p>Cada lectura incluye las condiciones del índice parcial que la resuelve: MongoDB solo usa un índice parcial si
 * la consulta las incluye.
 */
@Component
@Profile("mongo")
public class OpinionMongoAdapter implements OpinionRepositoryPort {

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc(FECHA), Sort.Order.asc("_id"));
    private static final Sort MAS_ANTIGUA_PRIMERO = Sort.by(Sort.Order.asc(FECHA), Sort.Order.asc("_id"));

    private final MongoTemplate mongo;

    public OpinionMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public Optional<Opinion> buscarPorId(String id) {
        return OpinionMongoMapper.objectId(id)
                .map(objectId -> mongo.findById(objectId, OpinionDocument.class))
                .map(OpinionMongoMapper::aDominio);
    }

    @Override
    public void guardar(Opinion opinion) {
        if (opinion.revision() == Opinion.REVISION_NUEVA) {
            insertar(opinion);
            return;
        }
        Query leida = query(where("_id").is(new ObjectId(opinion.id())).and(REVISION).is(opinion.revision()));
        if (mongo.replace(leida, OpinionMongoMapper.aDocumento(opinion, opinion.revision() + 1)).getMatchedCount() == 0) {
            throw conflicto(opinion);
        }
    }

    @Override
    public boolean existeDe(UUID turistaId, String ofertaId) {
        return OpinionMongoMapper.objectId(ofertaId)
                .map(oferta -> mongo.exists(query(where(OFERTA_ID).is(oferta).and(TURISTA_ID).is(turistaId)),
                        OpinionDocument.class))
                .orElse(false);
    }

    /** Lo resuelve el índice parcial {@code opiniones_aprobadas}, que también da el orden. */
    @Override
    public Pagina<Opinion> buscarAprobadas(String ofertaId, Paginacion paginacion) {
        Optional<ObjectId> oferta = OpinionMongoMapper.objectId(ofertaId);
        if (oferta.isEmpty()) {
            return new Pagina<>(List.of(), paginacion, 0);
        }
        return paginar(aprobadasDe(oferta.get()), MAS_RECIENTE_PRIMERO, paginacion);
    }

    /** Una agregación sobre el mismo índice parcial: cuenta y suma en la base, sin traer las opiniones. */
    @Override
    public ResumenCalificaciones resumirAprobadas(String ofertaId) {
        Optional<ObjectId> oferta = OpinionMongoMapper.objectId(ofertaId);
        if (oferta.isEmpty()) {
            return ResumenCalificaciones.SIN_OPINIONES;
        }
        Document resumen = mongo.getCollection(OpinionDocument.COLECCION).aggregate(List.of(
                        new Document("$match", aprobadasDe(oferta.get()).getQueryObject()),
                        new Document("$group", new Document("_id", null)
                                .append("total", new Document("$sum", 1))
                                .append("suma", new Document("$sum", "$" + CALIFICACION)))))
                .first();
        if (resumen == null) {
            return ResumenCalificaciones.SIN_OPINIONES;
        }
        return new ResumenCalificaciones(resumen.get("total", Number.class).longValue(),
                resumen.get("suma", Number.class).longValue());
    }

    /** Lo resuelve el índice {@code opiniones_del_turista}. */
    @Override
    public Pagina<Opinion> buscarDelTurista(UUID turistaId, Paginacion paginacion) {
        return paginar(query(where(TURISTA_ID).is(turistaId)), MAS_RECIENTE_PRIMERO, paginacion);
    }

    /** Lo resuelve el índice parcial {@code opiniones_en_moderacion}. */
    @Override
    public Pagina<Opinion> buscarEnModeracion(Paginacion paginacion) {
        return paginar(enModeracion(), MAS_ANTIGUA_PRIMERO, paginacion);
    }

    @Override
    public long eliminarDelTurista(UUID turistaId) {
        return mongo.remove(query(where(TURISTA_ID).is(turistaId)), OpinionDocument.class).getDeletedCount();
    }

    /** Condición del índice parcial {@code opiniones_aprobadas}. */
    static Document soloAprobadas() {
        return new Document(ESTADO, EstadoOpinion.APROBADA.name());
    }

    /** Condición del índice parcial {@code opiniones_en_moderacion}. */
    static Document soloEnModeracion() {
        return new Document(ESTADO, EstadoOpinion.EN_MODERACION.name());
    }

    private static Query aprobadasDe(ObjectId ofertaId) {
        return query(where(OFERTA_ID).is(ofertaId).and(ESTADO).is(EstadoOpinion.APROBADA.name()));
    }

    private static Query enModeracion() {
        return query(where(ESTADO).is(EstadoOpinion.EN_MODERACION.name()));
    }

    private Pagina<Opinion> paginar(Query filtro, Sort orden, Paginacion paginacion) {
        long total = mongo.count(filtro, OpinionDocument.class);
        List<Opinion> contenido = mongo.find(Query.of(filtro)
                        .with(orden)
                        .skip(paginacion.desplazamiento())
                        .limit(paginacion.tamano()), OpinionDocument.class).stream()
                .map(OpinionMongoMapper::aDominio)
                .toList();
        return new Pagina<>(contenido, paginacion, total);
    }

    /**
     * El índice único de {@code {ofertaId, turistaId}} o el de {@code _id} rechazó la inserción: si ya existe una
     * opinión del turista sobre la oferta es un duplicado (RF-61); si no, otra escritura insertó el mismo id.
     */
    private void insertar(Opinion opinion) {
        try {
            mongo.insert(OpinionMongoMapper.aDocumento(opinion, Opinion.REVISION_NUEVA + 1));
        } catch (DuplicateKeyException e) {
            if (existeDe(opinion.turistaId(), opinion.ofertaId())) {
                throw new OpinionDuplicadaException(opinion.turistaId(), opinion.ofertaId());
            }
            throw conflicto(opinion);
        }
    }

    private static ConflictoConcurrenciaException conflicto(Opinion opinion) {
        return new ConflictoConcurrenciaException("La opinión %s cambió desde que se leyó".formatted(opinion.id()));
    }
}
