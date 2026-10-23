package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.IntegracionMongoBase;
import co.edu.unimagdalena.samariantour.catalogo.compartido.config.MongoConfig;
import org.bson.BsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import static com.mongodb.client.model.Filters.eq;

/**
 * Base de las pruebas de los adaptadores de opiniones en MongoDB: solo la capa de persistencia del módulo, contra el
 * MongoDB de Testcontainers, con la misma configuración que el servicio (perfil {@code mongo}).
 */
@DataMongoTest
@Import({MongoConfig.class, IndicesOpinionesMongo.class, OpinionMongoAdapter.class,
        HabilitacionOpinionMongoAdapter.class, AvisoOpinionProcesadoMongoAdapter.class})
@ActiveProfiles("mongo")
abstract class BasePersistenciaOpinionesMongo extends IntegracionMongoBase {

    @Autowired
    protected MongoTemplate mongo;

    /** El documento tal como quedó en la base, con sus tipos BSON, sin pasar por el conversor de Spring. */
    protected BsonDocument documentoGuardado(String coleccion, Object id) {
        return mongo.getCollection(coleccion).withDocumentClass(BsonDocument.class).find(eq("_id", id)).first();
    }
}
