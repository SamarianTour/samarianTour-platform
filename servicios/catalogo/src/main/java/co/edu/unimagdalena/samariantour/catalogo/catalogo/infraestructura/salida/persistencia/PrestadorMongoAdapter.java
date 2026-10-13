package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PrestadorMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument.RNT_ACTIVO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument.VERSION;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Réplica de la habilitación RNT en la colección {@code prestadores} de MongoDB (DC-02).
 */
@Component
@Profile("mongo")
public class PrestadorMongoAdapter implements PrestadorCatalogoRepositoryPort {

    private final MongoTemplate mongo;

    public PrestadorMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<PrestadorCatalogo> buscarPorId(UUID prestadorId) {
        return Optional.ofNullable(mongo.findById(prestadorId, PrestadorDocument.class))
                .map(PrestadorMongoMapper::aDominio);
    }

    /**
     * Un upsert filtrado por {@code version < nueva}, en una sola operación atómica (ADR-05). Si no hay réplica, la
     * crea. Si la guardada tiene una versión igual o mayor, el filtro no la encuentra, el upsert intenta crear otra con
     * el mismo {@code _id} y la base lo rechaza: eso es el conflicto.
     */
    @Override
    public void guardar(PrestadorCatalogo prestador) {
        try {
            mongo.upsert(query(where("_id").is(prestador.prestadorId()).and(VERSION).lt(prestador.version())),
                    new Update().set(RNT_ACTIVO, prestador.rntActivo()).set(VERSION, prestador.version()),
                    PrestadorDocument.class);
        } catch (DuplicateKeyException e) {
            throw new ConflictoConcurrenciaException("La habilitación del prestador %s ya tiene la versión %d o una mayor"
                    .formatted(prestador.prestadorId(), prestador.version()));
        }
    }
}
