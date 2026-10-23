package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.AvisoOpinionProcesadoDocument;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AvisoOpinionProcesadoMongoAdapterIT extends BasePersistenciaOpinionesMongo {

    @Autowired
    private AvisoOpinionProcesadoMongoAdapter avisos;

    @BeforeEach
    void vaciar() {
        mongo.remove(new Query(), AvisoOpinionProcesadoDocument.class);
    }

    @Test
    @DisplayName("un aviso marcado queda procesado para cualquier réplica, y marcarlo otra vez no falla")
    void marcaYConsulta() {
        assertThat(avisos.fueProcesado("aviso-1")).isFalse();

        avisos.marcarProcesado("aviso-1");

        assertThatCode(() -> avisos.marcarProcesado("aviso-1")).doesNotThrowAnyException();
        assertThat(avisos.fueProcesado("aviso-1")).isTrue();
        assertThat(mongo.count(new Query(), AvisoOpinionProcesadoDocument.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("el idAviso se guarda como texto aunque tenga formato ObjectId, en su propia colección")
    void idComoTexto() {
        String conFormatoObjectId = new ObjectId().toHexString();

        avisos.marcarProcesado(conFormatoObjectId);

        assertThat(documentoGuardado(AvisoOpinionProcesadoDocument.COLECCION, conFormatoObjectId).get("_id")
                .getBsonType()).isEqualTo(BsonType.STRING);
        assertThat(mongo.getCollection("avisos_procesados").countDocuments(new Document("_id", conFormatoObjectId)))
                .as("no usa el registro de los avisos de ofertas").isZero();
    }
}
