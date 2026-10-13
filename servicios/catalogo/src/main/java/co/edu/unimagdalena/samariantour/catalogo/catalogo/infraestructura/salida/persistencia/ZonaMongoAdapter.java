package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ZonaMongoMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Zonas en la colección {@code zonas} de MongoDB. En el prototipo solo se leen; las carga la precarga (DC-18).
 */
@Component
@Profile("mongo")
public class ZonaMongoAdapter implements ZonaRepositoryPort {

    private final MongoTemplate mongo;

    public ZonaMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<Zona> buscarPorId(String id) {
        return ConversionesMongo.objectId(id)
                .map(objectId -> mongo.findById(objectId, ZonaDocument.class))
                .map(ZonaMongoMapper::aDominio);
    }

    @Override
    public List<Zona> buscarActivas() {
        return mongo.find(query(where(ZonaDocument.ACTIVA).is(true)).with(Sort.by(ZonaDocument.NOMBRE)),
                        ZonaDocument.class).stream()
                .map(ZonaMongoMapper::aDominio)
                .toList();
    }
}
