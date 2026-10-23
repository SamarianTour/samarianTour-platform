package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.FotoSubidaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.FotoSubidaMongoMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.FotoSubidaDocument.ULTIMO_USO;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Registro de las fotos subidas en la colección {@code fotos} de MongoDB (DC-47). Cada operación es una sola escritura
 * atómica: marcar el uso con {@code $max}, para que nunca retroceda, y borrar con el uso leído en el filtro.
 */
@Component
@Profile("mongo")
public class FotoSubidaMongoAdapter implements FotoSubidaRepositoryPort {

    private final MongoTemplate mongo;

    public FotoSubidaMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void registrar(FotoSubida foto) {
        mongo.insert(FotoSubidaMongoMapper.aDocumento(foto));
    }

    @Override
    public List<FotoSubida> buscar(Collection<Foto> fotos) {
        return mongo.find(query(where("_id").in(ids(fotos))), FotoSubidaDocument.class).stream()
                .map(FotoSubidaMongoMapper::aDominio)
                .toList();
    }

    @Override
    public void marcarEnUso(Collection<Foto> fotos, Instant ahora) {
        mongo.updateMulti(query(where("_id").in(ids(fotos))), new Update().max(ULTIMO_USO, ahora),
                FotoSubidaDocument.class);
    }

    /** Usa el índice {@code fotos_por_ultimo_uso}. */
    @Override
    public List<FotoSubida> sinUsoDesde(Instant limite, int maximo) {
        return mongo.find(query(where(ULTIMO_USO).lt(limite))
                                .with(Sort.by(ULTIMO_USO, "_id"))
                                .limit(maximo),
                        FotoSubidaDocument.class).stream()
                .map(FotoSubidaMongoMapper::aDominio)
                .toList();
    }

    /** MongoDB guarda milisegundos, igual que el uso leído de la base, así que la comparación es exacta. */
    @Override
    public boolean eliminarSiSigueSinUso(FotoSubida leida) {
        return mongo.remove(query(where("_id").is(leida.foto().id()).and(ULTIMO_USO).lte(leida.ultimoUso())),
                        FotoSubidaDocument.class)
                .getDeletedCount() == 1;
    }

    private static List<String> ids(Collection<Foto> fotos) {
        return fotos.stream().map(Foto::id).distinct().toList();
    }
}
