package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.AvisoProcesadoPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.AvisoProcesadoDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Avisos del núcleo ya aplicados, en la colección {@code avisos_procesados} de MongoDB. Como la base es compartida,
 * descarta los repetidos aunque lleguen a otra réplica (DC-04, ADR-05).
 */
@Component
@Profile("mongo")
public class AvisoProcesadoMongoAdapter implements AvisoProcesadoPort {

    private final MongoTemplate mongo;

    public AvisoProcesadoMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean fueProcesado(String idAviso) {
        return mongo.exists(query(where("_id").is(idAviso)), AvisoProcesadoDocument.class);
    }

    /** Si otra réplica ya lo marcó, el {@code _id} repetido se rechaza y no hay nada más que hacer. */
    @Override
    public void marcarProcesado(String idAviso) {
        try {
            mongo.insert(new AvisoProcesadoDocument(idAviso));
        } catch (DuplicateKeyException e) {
            // Ya estaba marcado.
        }
    }
}
