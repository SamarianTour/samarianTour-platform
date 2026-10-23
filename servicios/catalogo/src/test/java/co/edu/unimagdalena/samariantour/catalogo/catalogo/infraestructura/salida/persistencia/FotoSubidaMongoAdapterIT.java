package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoFotoSubidaRepository;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.FotoSubidaDocument;
import org.bson.BsonBinarySubType;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El registro de fotos contra un MongoDB real: el mismo contrato que el adaptador en memoria, y el formato de la
 * colección {@code fotos}.
 */
class FotoSubidaMongoAdapterIT extends BasePersistenciaMongo implements ContratoFotoSubidaRepository {

    @Autowired
    private FotoSubidaMongoAdapter registro;

    @Autowired
    private IndicesMongo indices;

    @BeforeEach
    void vaciar() {
        mongo.remove(new Query(), FotoSubidaDocument.class);
        indices.crear();
    }

    @Override
    public FotoSubidaRepositoryPort registro() {
        return registro;
    }

    @Test
    @DisplayName("guarda el id como texto, el prestador como UUID estándar, el destino por nombre y el uso como fecha")
    void formato() {
        Foto foto = Foto.nueva();
        registro.registrar(new FotoSubida(foto, PRESTADOR, DestinoFoto.OFERTAS, SUBIDA));

        BsonDocument guardado = documentoGuardado(FotoSubidaDocument.COLECCION, foto.id());

        assertThat(guardado.getString("_id").getValue()).isEqualTo(foto.id());
        assertThat(guardado.getBinary("prestadorId").getType()).isEqualTo(BsonBinarySubType.UUID_STANDARD.getValue());
        assertThat(guardado.getString("destino").getValue()).isEqualTo("OFERTAS");
        assertThat(guardado.getDateTime(FotoSubidaDocument.ULTIMO_USO).getValue()).isEqualTo(SUBIDA.toEpochMilli());
        assertThat(guardado.containsKey("_class")).isFalse();
    }
}
