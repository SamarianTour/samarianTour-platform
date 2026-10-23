package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoPerfilRepository;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PerfilMongoMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

/**
 * El adaptador de perfiles contra un MongoDB real: el mismo contrato que el adaptador en memoria.
 */
class PerfilMongoAdapterIT extends BasePersistenciaMongo implements ContratoPerfilRepository {

    @Autowired
    private PerfilMongoAdapter perfiles;

    @Autowired
    private IndicesMongo indices;

    @BeforeEach
    void cargarPerfilesDeDemostracion() {
        mongo.remove(new Query(), PerfilDocument.class);
        indices.crear();
        mongo.insertAll(DatosSantaMarta.perfiles().stream()
                .map(perfil -> PerfilMongoMapper.aDocumento(new ObjectId(), perfil, PerfilPublico.REVISION_NUEVA + 1))
                .toList());
    }

    @Override
    public PerfilRepositoryPort perfiles() {
        return perfiles;
    }
}
