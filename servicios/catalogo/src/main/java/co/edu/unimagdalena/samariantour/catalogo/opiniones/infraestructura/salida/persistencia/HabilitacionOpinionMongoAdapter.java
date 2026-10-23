package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.mapper.OpinionMongoMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument.OFERTA_ID;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument.TURISTA_ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Habilitaciones para opinar en la colección {@code habilitaciones_opinion} (DC-40). El índice único
 * {@code {turistaId, ofertaId}} hace que registrar la misma habilitación dos veces, desde cualquier réplica, deje una.
 */
@Component
@Profile("mongo")
public class HabilitacionOpinionMongoAdapter implements HabilitacionOpinionRepositoryPort {

    private final MongoTemplate mongo;

    public HabilitacionOpinionMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean registrar(HabilitacionOpinion habilitacion) {
        try {
            mongo.insert(OpinionMongoMapper.aDocumento(habilitacion));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public boolean existe(UUID turistaId, String ofertaId) {
        return OpinionMongoMapper.objectId(ofertaId)
                .map(oferta -> mongo.exists(query(where(TURISTA_ID).is(turistaId).and(OFERTA_ID).is(oferta)),
                        HabilitacionOpinionDocument.class))
                .orElse(false);
    }

    @Override
    public long eliminarDelTurista(UUID turistaId) {
        return mongo.remove(query(where(TURISTA_ID).is(turistaId)), HabilitacionOpinionDocument.class)
                .getDeletedCount();
    }
}
