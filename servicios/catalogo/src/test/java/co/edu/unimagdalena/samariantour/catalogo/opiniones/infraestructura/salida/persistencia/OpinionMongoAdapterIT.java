package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.ContratoOpinionRepository;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument;
import org.bson.BsonDocument;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.ANDRES;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_ANDRES_KAYAK;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_RECHAZADA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los adaptadores de opiniones contra un MongoDB real: cumplen el mismo contrato que los adaptadores en memoria y,
 * además, guardan el formato del modelo documental (DC-30) y resuelven sus lecturas con índices (DC-46).
 */
class OpinionMongoAdapterIT extends BasePersistenciaOpinionesMongo implements ContratoOpinionRepository {

    @Autowired
    private OpinionMongoAdapter opiniones;

    @Autowired
    private HabilitacionOpinionMongoAdapter habilitaciones;

    @Autowired
    private IndicesOpinionesMongo indices;

    @BeforeEach
    void cargarDatosDeDemostracion() {
        mongo.remove(new Query(), OpinionDocument.class);
        mongo.remove(new Query(), HabilitacionOpinionDocument.class);
        indices.crear();
        DatosOpiniones.opiniones().forEach(opiniones::guardar);
        DatosOpiniones.habilitaciones().forEach(habilitaciones::registrar);
    }

    @Override
    public OpinionRepositoryPort opiniones() {
        return opiniones;
    }

    @Override
    public HabilitacionOpinionRepositoryPort habilitaciones() {
        return habilitaciones;
    }

    @Test
    @DisplayName("sin _class, con la oferta como ObjectId, el turista como UUID estándar y las fechas como fecha")
    void formatoDelDocumento() {
        BsonDocument rechazada = documentoGuardado(OpinionDocument.COLECCION, new ObjectId(OPINION_RECHAZADA));
        BsonDocument enModeracion = documentoGuardado(OpinionDocument.COLECCION, new ObjectId(OPINION_ANDRES_KAYAK));

        assertThat(rechazada.containsKey("_class")).isFalse();
        assertThat(rechazada.get("ofertaId").getBsonType()).isEqualTo(BsonType.OBJECT_ID);
        assertThat(rechazada.getBinary("turistaId").getType()).isEqualTo((byte) 4);
        assertThat(rechazada.get("fecha").getBsonType()).isEqualTo(BsonType.DATE_TIME);
        assertThat(rechazada.get("revisadaEn").getBsonType()).isEqualTo(BsonType.DATE_TIME);
        assertThat(rechazada.getString("estado").getValue()).isEqualTo("RECHAZADA");
        assertThat(rechazada.get("revision").getBsonType()).isEqualTo(BsonType.INT64);
        assertThat(enModeracion.containsKey("motivoRechazo")).as("los campos sin valor no se guardan").isFalse();
        assertThat(enModeracion.containsKey("revisadaEn")).isFalse();
    }

    @Nested
    @DisplayName("Índices (DC-46)")
    class Indices {

        @Test
        @DisplayName("la unicidad de la opinión y de la habilitación la garantiza la base (RF-61)")
        void indicesUnicos() {
            List<IndexInfo> deOpiniones = mongo.indexOps(OpinionDocument.class).getIndexInfo();
            List<IndexInfo> deHabilitaciones = mongo.indexOps(HabilitacionOpinionDocument.class).getIndexInfo();

            assertThat(deOpiniones).filteredOn(indice -> indice.getName().equals("opinion_unica"))
                    .singleElement().matches(IndexInfo::isUnique);
            assertThat(deHabilitaciones).filteredOn(indice -> indice.getName().equals("habilitacion_unica"))
                    .singleElement().matches(IndexInfo::isUnique);
            assertThat(deOpiniones).filteredOn(indice -> indice.getName().equals("opiniones_aprobadas"))
                    .singleElement().satisfies(indice ->
                            assertThat(indice.getPartialFilterExpression()).contains("APROBADA"));
        }

        @Test
        @DisplayName("la lectura pública usa el índice parcial opiniones_aprobadas")
        void lecturaPublica() {
            String plan = planDe(new Document("ofertaId", new ObjectId(CAMINATA_CABO_SAN_JUAN))
                            .append("estado", "APROBADA"),
                    new Document("fecha", -1).append("_id", 1));

            assertThat(plan).contains("opiniones_aprobadas").doesNotContain("COLLSCAN");
        }

        @Test
        @DisplayName("la cola del administrador usa el índice parcial opiniones_en_moderacion")
        void colaDeModeracion() {
            String plan = planDe(new Document("estado", "EN_MODERACION"), new Document("fecha", 1).append("_id", 1));

            assertThat(plan).contains("opiniones_en_moderacion").doesNotContain("COLLSCAN");
        }

        @Test
        @DisplayName("las opiniones del turista usan el índice opiniones_del_turista")
        void delTurista() {
            String plan = planDe(new Document("turistaId", ANDRES), new Document("fecha", -1).append("_id", 1));

            assertThat(plan).contains("opiniones_del_turista").doesNotContain("COLLSCAN");
        }

        private String planDe(Document filtro, Document orden) {
            return mongo.getCollection(OpinionDocument.COLECCION).find(filtro).sort(orden).explain().toJson();
        }
    }
}
