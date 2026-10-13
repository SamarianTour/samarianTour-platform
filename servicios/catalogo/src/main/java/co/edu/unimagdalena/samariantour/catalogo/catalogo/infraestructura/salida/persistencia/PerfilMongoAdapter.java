package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PerfilMongoMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Perfiles públicos en la colección {@code perfiles} de MongoDB. En el prototipo solo se leen.
 */
@Component
@Profile("mongo")
public class PerfilMongoAdapter implements PerfilRepositoryPort {

    private final MongoTemplate mongo;

    public PerfilMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<PerfilPublico> buscarPorPrestador(UUID prestadorId) {
        return Optional.ofNullable(mongo.findOne(query(where(PerfilDocument.PRESTADOR_ID).is(prestadorId)),
                        PerfilDocument.class))
                .map(PerfilMongoMapper::aDominio);
    }
}
