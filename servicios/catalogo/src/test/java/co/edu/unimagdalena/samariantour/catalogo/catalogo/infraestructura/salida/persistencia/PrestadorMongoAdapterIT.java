package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoPrestadorRepository;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La réplica de habilitación contra un MongoDB real: cumple el mismo contrato que el adaptador en memoria.
 */
class PrestadorMongoAdapterIT extends BasePersistenciaMongo implements ContratoPrestadorRepository {

    @Autowired
    private PrestadorMongoAdapter prestadores;

    @BeforeEach
    void cargarPrestador() {
        mongo.remove(new Query(), PrestadorDocument.class);
        prestadores.guardar(new PrestadorCatalogo(PRESTADOR, true, 2));
    }

    @Override
    public PrestadorCatalogoRepositoryPort prestadores() {
        return prestadores;
    }

    @Test
    @DisplayName("el _id es el UUID del núcleo, guardado como binario estándar (subtipo 4) y sin _class")
    void formato() {
        BsonDocument guardado = documentoGuardado(PrestadorDocument.COLECCION, PRESTADOR);

        assertThat(guardado.getBinary("_id").getType()).isEqualTo((byte) 4);
        assertThat(guardado.getBoolean("rntActivo").getValue()).isTrue();
        assertThat(guardado.getInt64("version").getValue()).isEqualTo(2);
        assertThat(guardado.containsKey("_class")).isFalse();
    }
}
