package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoOfertaRepository;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
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

import java.math.BigDecimal;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PAQUETE_TAYRONA_MAR_Y_SELVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PASEO_LANCHA_PLAYA_BLANCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TAYRONA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * El adaptador de ofertas contra un MongoDB real: cumple el mismo contrato que el adaptador en memoria y, además,
 * guarda el formato del modelo documental (DC-16, DC-30) y resuelve la búsqueda con los índices de QAS-07 (DC-17).
 */
class OfertaMongoAdapterIT extends BasePersistenciaMongo implements ContratoOfertaRepository {

    @Autowired
    private OfertaMongoAdapter ofertas;

    @Autowired
    private IndicesMongo indices;

    @BeforeEach
    void cargarDatosDeDemostracion() {
        mongo.remove(new Query(), OfertaDocument.class);
        indices.crear();
        DatosSantaMarta.ofertas().forEach(ofertas::guardar);
    }

    @Override
    public OfertaRepositoryPort ofertas() {
        return ofertas;
    }

    @Nested
    @DisplayName("Formato del documento (DC-16)")
    class Formato {

        @Test
        @DisplayName("sin _class, con precio Decimal128, referencias ObjectId, UUID estándar y horas HH:mm")
        void tiposDelModeloDocumental() {
            BsonDocument caminata = documentoGuardado(OfertaDocument.COLECCION, new ObjectId(CAMINATA_CABO_SAN_JUAN));
            BsonDocument publicada = caminata.getDocument("publicada");

            assertThat(caminata.containsKey("_class")).isFalse();
            assertThat(caminata.getString("tipo").getValue()).isEqualTo("ACTIVIDAD");
            assertThat(caminata.getBinary("prestadorId").getType()).isEqualTo((byte) 4);
            assertThat(caminata.get("revision").getBsonType()).isEqualTo(BsonType.INT64);
            assertThat(caminata.get("versionHabilitacion").getBsonType()).isEqualTo(BsonType.INT64);
            assertThat(publicada.get("precioReferencia").getBsonType()).isEqualTo(BsonType.DECIMAL128);
            assertThat(publicada.get("zonaId").getBsonType()).isEqualTo(BsonType.OBJECT_ID);
            assertThat(publicada.get("enviadaEn").getBsonType()).isEqualTo(BsonType.DATE_TIME);
            assertThat(publicada.getArray("horariosSalida").get(0).asString().getValue()).matches("\\d{2}:\\d{2}");
            BsonDocument ubicacion = publicada.getDocument("ubicacion");
            assertThat(ubicacion.getString("type").getValue()).isEqualTo("Point");
            assertThat(ubicacion.getArray("coordinates")).as("GeoJSON guarda [longitud, latitud]")
                    .extracting(valor -> valor.asDouble().getValue())
                    .containsExactly(-73.9647, 11.3247);
        }

        @Test
        @DisplayName("cada versión guarda solo los campos de su tipo; un paquete referencia sus actividades por ObjectId")
        void camposDelTipo() {
            BsonDocument paquete = documentoGuardado(OfertaDocument.COLECCION,
                    new ObjectId(PAQUETE_TAYRONA_MAR_Y_SELVA)).getDocument("publicada");

            assertThat(paquete.getArray("actividadIds")).allSatisfy(id ->
                    assertThat(id.getBsonType()).isEqualTo(BsonType.OBJECT_ID));
            assertThat(paquete.containsKey("tipoActividad")).isFalse();
            assertThat(paquete.containsKey("accesibleSillaRuedas")).isFalse();
            assertThat(paquete.containsKey("motivoRechazo")).isFalse();
        }

        @Test
        @DisplayName("una oferta sin aprobar no tiene publicada, y una sin edición en curso no tiene pendiente (DC-06)")
        void versionesAusentes() {
            assertThat(documentoGuardado(OfertaDocument.COLECCION, new ObjectId(PASEO_LANCHA_PLAYA_BLANCA))
                    .containsKey("publicada")).isFalse();
            assertThat(documentoGuardado(OfertaDocument.COLECCION, new ObjectId(PAQUETE_TAYRONA_MAR_Y_SELVA))
                    .containsKey("pendiente")).isFalse();
        }
    }

    @Nested
    @DisplayName("Índices (DC-17, QAS-07)")
    class Indices {

        @Test
        @DisplayName("los índices de búsqueda son parciales: solo guardan las ofertas visibles")
        void indicesParciales() {
            List<IndexInfo> creados = mongo.indexOps(OfertaDocument.class).getIndexInfo();

            assertThat(creados).extracting(IndexInfo::getName)
                    .contains("busqueda_tipo_zona_precio", "busqueda_precio", "ofertas_del_prestador");
            assertThat(creados).filteredOn(indice -> indice.getName().startsWith("busqueda_"))
                    .allSatisfy(indice -> assertThat(indice.getPartialFilterExpression())
                            .contains("rntPrestadorActivo", "retirada", "publicada"));
        }

        @Test
        @DisplayName("la búsqueda por tipo, zona y precio usa un índice y no recorre la colección")
        void busquedaConFiltros() {
            CriteriosBusqueda criterios = new CriteriosBusqueda(null, TipoOferta.ACTIVIDAD, TAYRONA,
                    new BigDecimal("50000"), new BigDecimal("200000"), null, null, false, Paginacion.de(null, null));

            assertThat(planDe(criterios)).contains("IXSCAN").doesNotContain("COLLSCAN");
        }

        @Test
        @DisplayName("la búsqueda sin filtros, que incluye paquetes, también usa un índice")
        void busquedaSinFiltros() {
            CriteriosBusqueda criterios = new CriteriosBusqueda(null, null, null, null, null, null, null, false,
                    Paginacion.de(null, null));

            assertThat(planDe(criterios)).contains("IXSCAN").doesNotContain("COLLSCAN");
        }

        @Test
        @DisplayName("listar las ofertas del prestador usa el índice {prestadorId, _id}")
        void ofertasDelPrestador() {
            Document plan = mongo.getCollection(OfertaDocument.COLECCION)
                    .find(new Document("prestadorId", AGENCIA))
                    .sort(new Document("_id", 1))
                    .explain();

            assertThat(plan.toJson()).contains("ofertas_del_prestador").doesNotContain("COLLSCAN");
        }

        @Test
        @DisplayName("listar las ofertas en moderación usa el índice parcial en_moderacion (DC-38)")
        void ofertasEnModeracion() {
            Document plan = mongo.getCollection(OfertaDocument.COLECCION)
                    .find(OfertaMongoAdapter.enModeracion())
                    .sort(new Document(OfertaMongoAdapter.PENDIENTE_ENVIADA_EN, 1).append("_id", 1))
                    .explain();

            assertThat(plan.toJson()).contains("en_moderacion").doesNotContain("COLLSCAN");
        }

        @Test
        @DisplayName("saber si una foto está en uso consulta cada versión con su índice de fotos (DC-47)")
        void fotosEnUso() {
            for (String campo : List.of(OfertaMongoAdapter.PUBLICADA_FOTOS, OfertaMongoAdapter.PENDIENTE_FOTOS)) {
                Document plan = mongo.getCollection(OfertaDocument.COLECCION)
                        .find(new Document(campo, new Document("$in", List.of("3f2504e0-4f89-11d3-9a0c-0305e82c3301")))
                                .append("retirada", false))
                        .explain();

                assertThat(plan.toJson()).contains("IXSCAN").doesNotContain("COLLSCAN");
            }
        }

        private String planDe(CriteriosBusqueda criterios) {
            ObjectId zonaId = criterios.zonaId() == null ? null : new ObjectId(criterios.zonaId());
            return mongo.getCollection(OfertaDocument.COLECCION)
                    .aggregate(BusquedaOfertasMongo.etapas(criterios, zonaId))
                    .explain()
                    .toJson();
        }
    }

    @Test
    @DisplayName("una oferta nueva queda con la revisión 1 y cada guardado la sube en uno (ADR-05)")
    void revisiones() {
        assertThat(ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow().revision()).isEqualTo(1);

        ofertas.guardar(ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow());

        assertThat(mongo.findById(new ObjectId(CAMINATA_CABO_SAN_JUAN), OfertaDocument.class).revision())
                .isEqualTo(2);
    }
}
