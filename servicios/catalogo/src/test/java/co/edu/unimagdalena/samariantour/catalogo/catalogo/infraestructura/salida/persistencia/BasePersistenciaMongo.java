package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

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
 * Base de las pruebas de los adaptadores de MongoDB: solo la capa de persistencia, contra el MongoDB de
 * Testcontainers, con la misma configuración que el servicio (perfil {@code mongo}). Todas las clases comparten el
 * contexto de Spring y el contenedor.
 */
@DataMongoTest
@Import({MongoConfig.class, IndicesMongo.class, OfertaMongoAdapter.class, ZonaMongoAdapter.class,
        PerfilMongoAdapter.class, PrestadorMongoAdapter.class, AvisoProcesadoMongoAdapter.class,
        FotoSubidaMongoAdapter.class})
@ActiveProfiles("mongo")
abstract class BasePersistenciaMongo extends IntegracionMongoBase {

    @Autowired
    protected MongoTemplate mongo;

    /** El documento tal como quedó en la base, con sus tipos BSON, sin pasar por el conversor de Spring. */
    protected BsonDocument documentoGuardado(String coleccion, Object id) {
        return mongo.getCollection(coleccion).withDocumentClass(BsonDocument.class).find(eq("_id", id)).first();
    }
}
