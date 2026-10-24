package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ZonaMongoMapper;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Zonas en la colección {@code zonas} de MongoDB (DC-18, DC-49). El nombre único lo garantiza el índice
 * {@code zona_por_nombre} sobre {@code nombreClave}: si otra réplica guarda a la vez una zona con el mismo nombre, una de
 * las dos escrituras falla.
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
        return buscar(query(where(ZonaDocument.ACTIVA).is(true)));
    }

    @Override
    public List<Zona> buscarTodas() {
        return buscar(new Query());
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    /** Reemplaza el documento completo, o lo inserta si no existe. */
    @Override
    public void guardar(Zona zona) {
        try {
            mongo.save(ZonaMongoMapper.aDocumento(zona));
        } catch (DuplicateKeyException e) {
            throw new ZonaDuplicadaException(zona.nombre());
        }
    }

    private List<Zona> buscar(Query consulta) {
        return mongo.find(consulta.with(Sort.by(ZonaDocument.NOMBRE)), ZonaDocument.class).stream()
                .map(ZonaMongoMapper::aDominio)
                .toList();
    }
}
