package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.AvisoProcesadoDocument;
import org.bson.BsonType;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AvisoProcesadoMongoAdapterIT extends BasePersistenciaMongo {

    @Autowired
    private AvisoProcesadoMongoAdapter avisos;

    @BeforeEach
    void vaciar() {
        mongo.remove(new Query(), AvisoProcesadoDocument.class);
    }

    @Test
    @DisplayName("un aviso marcado queda procesado para cualquier réplica que consulte la base")
    void marcaYConsulta() {
        assertThat(avisos.fueProcesado("aviso-1")).isFalse();

        avisos.marcarProcesado("aviso-1");

        assertThat(avisos.fueProcesado("aviso-1")).isTrue();
        assertThat(avisos.fueProcesado("aviso-2")).isFalse();
    }

    @Test
    @DisplayName("marcar un aviso que otra réplica ya marcó no falla: el _id repetido se tolera (ADR-05)")
    void marcarDosVeces() {
        avisos.marcarProcesado("aviso-1");

        assertThatCode(() -> avisos.marcarProcesado("aviso-1")).doesNotThrowAnyException();
        assertThat(mongo.count(new Query(), AvisoProcesadoDocument.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("el idAviso se guarda como texto aunque tenga formato ObjectId")
    void idComoTexto() {
        String conFormatoObjectId = new ObjectId().toHexString();

        avisos.marcarProcesado(conFormatoObjectId);

        assertThat(documentoGuardado(AvisoProcesadoDocument.COLECCION, conFormatoObjectId).get("_id").getBsonType())
                .isEqualTo(BsonType.STRING);
        assertThat(avisos.fueProcesado(conFormatoObjectId)).isTrue();
    }
}
