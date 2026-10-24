package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoZonaRepository;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ZonaMongoMapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TAGANGA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * El contrato de las zonas contra un MongoDB real, más lo propio del documento: la clave del nombre y su índice único
 * (DC-49).
 */
class ZonaMongoAdapterIT extends BasePersistenciaMongo implements ContratoZonaRepository {

    @Autowired
    private ZonaMongoAdapter zonas;

    @Autowired
    private IndicesMongo indices;

    @BeforeEach
    void cargarZonasDeDemostracion() {
        mongo.remove(new Query(), ZonaDocument.class);
        indices.crear();
        mongo.insertAll(DatosSantaMarta.zonas().stream().map(ZonaMongoMapper::aDocumento).toList());
    }

    @Override
    public ZonaRepositoryPort zonas() {
        return zonas;
    }

    @Test
    @DisplayName("guarda la clave del nombre junto al nombre")
    void claveDelNombre() {
        assertThat(documentoGuardado(ZonaDocument.COLECCION, new ObjectId(TAGANGA))
                .getString(ZonaDocument.NOMBRE_CLAVE).getValue()).isEqualTo("taganga");
    }

    @Test
    @DisplayName("una base creada antes de DC-49, sin la clave del nombre, la recibe al crear los índices")
    void completaLaClaveDeUnaBaseAnterior() {
        // Sin el índice primero: con él, dos zonas sin clave ya cuentan como repetidas.
        mongo.indexOps(ZonaDocument.class).dropIndex("zona_por_nombre");
        mongo.getCollection(ZonaDocument.COLECCION).updateMany(new Document(),
                new Document("$unset", new Document(ZonaDocument.NOMBRE_CLAVE, "")));

        indices.crear();

        for (Zona zona : DatosSantaMarta.zonas()) {
            assertThat(documentoGuardado(ZonaDocument.COLECCION, new ObjectId(zona.id()))
                    .getString(ZonaDocument.NOMBRE_CLAVE).getValue()).isEqualTo(zona.claveNombre());
        }
        assertThat(mongo.indexOps(ZonaDocument.class).getIndexInfo())
                .anySatisfy(indice -> {
                    assertThat(indice.getName()).isEqualTo("zona_por_nombre");
                    assertThat(indice.isUnique()).isTrue();
                });
    }
}
