package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PerfilMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument.PRESTADOR_ID;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument.REVISION;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Perfiles públicos en la colección {@code perfiles} de MongoDB (RF-24, RF-64), con bloqueo optimista por revisión
 * como las ofertas (ADR-05).
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
        return Optional.ofNullable(mongo.findOne(query(where(PRESTADOR_ID).is(prestadorId)), PerfilDocument.class))
                .map(PerfilMongoMapper::aDominio);
    }

    /**
     * Un perfil nuevo se inserta: el índice único de {@code prestadorId} impide un segundo perfil del mismo
     * prestador. Uno guardado se actualiza filtrando por la revisión que se leyó; si cambió, no encuentra el documento.
     */
    @Override
    public void guardar(PerfilPublico perfil) {
        if (perfil.revision() == PerfilPublico.REVISION_NUEVA) {
            try {
                mongo.insert(PerfilMongoMapper.aDocumento(new ObjectId(), perfil, PerfilPublico.REVISION_NUEVA + 1));
            } catch (DuplicateKeyException e) {
                throw conflicto(perfil);
            }
            return;
        }
        Update cambios = new Update()
                .set(PerfilDocument.NOMBRE_COMERCIAL, perfil.nombreComercial())
                .set(PerfilDocument.DESCRIPCION, perfil.descripcion())
                .set(PerfilDocument.FOTOS, perfil.fotos().stream().map(Foto::id).toList())
                .set(PerfilDocument.HORARIOS_ATENCION, perfil.horariosAtencion())
                .set(PerfilDocument.SITIO_WEB, perfil.sitioWeb())
                .set(PerfilDocument.REDES_SOCIALES, perfil.redesSociales())
                .set(REVISION, perfil.revision() + 1);
        long actualizados = mongo.updateFirst(
                        query(where(PRESTADOR_ID).is(perfil.prestadorId()).and(REVISION).is(perfil.revision())),
                        cambios, PerfilDocument.class)
                .getMatchedCount();
        if (actualizados == 0) {
            throw conflicto(perfil);
        }
    }

    private static ConflictoConcurrenciaException conflicto(PerfilPublico perfil) {
        return new ConflictoConcurrenciaException(
                "El perfil del prestador %s cambió desde que se leyó".formatted(perfil.prestadorId()));
    }
}
