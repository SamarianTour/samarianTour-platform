package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.stereotype.Component;

import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.FECHA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.OFERTA_ID;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument.TURISTA_ID;

/**
 * Crea los índices del módulo de opiniones al arrancar (DC-46). Son del módulo y no van en {@code IndicesMongo} del
 * catálogo, que no puede depender de opiniones (DC-40). Crear un índice que ya existe no hace nada, así que todas las
 * réplicas pueden hacerlo.
 *
 * <ul>
 *   <li>{@code opinion_unica}: único {@code {ofertaId, turistaId}}, una opinión por turista y oferta (RF-61).</li>
 *   <li>{@code opiniones_aprobadas}: parcial, solo las aprobadas, {@code {ofertaId, fecha desc, _id}}, para la lectura
 *       pública de la más reciente a la más antigua y para el resumen (RF-59).</li>
 *   <li>{@code opiniones_en_moderacion}: parcial, solo las que esperan decisión, {@code {fecha, _id}}, para la cola
 *       del administrador (RF-80).</li>
 *   <li>{@code opiniones_del_turista}: {@code {turistaId, fecha desc, _id}}, para "Mis opiniones" y para la
 *       supresión (DC-45).</li>
 *   <li>{@code habilitacion_unica}: único {@code {turistaId, ofertaId}} en {@code habilitaciones_opinion}; su prefijo
 *       también resuelve la supresión.</li>
 * </ul>
 */
@Component
@Profile("mongo")
public class IndicesOpinionesMongo implements InitializingBean {

    private final MongoTemplate mongo;

    public IndicesOpinionesMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void afterPropertiesSet() {
        crear();
    }

    public void crear() {
        IndexOperations opiniones = mongo.indexOps(OpinionDocument.class);
        opiniones.createIndex(new Index()
                .named("opinion_unica")
                .on(OFERTA_ID, Sort.Direction.ASC)
                .on(TURISTA_ID, Sort.Direction.ASC)
                .unique());
        opiniones.createIndex(new Index()
                .named("opiniones_aprobadas")
                .on(OFERTA_ID, Sort.Direction.ASC)
                .on(FECHA, Sort.Direction.DESC)
                .on("_id", Sort.Direction.ASC)
                .partial(PartialIndexFilter.of(OpinionMongoAdapter.soloAprobadas())));
        opiniones.createIndex(new Index()
                .named("opiniones_en_moderacion")
                .on(FECHA, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .partial(PartialIndexFilter.of(OpinionMongoAdapter.soloEnModeracion())));
        opiniones.createIndex(new Index()
                .named("opiniones_del_turista")
                .on(TURISTA_ID, Sort.Direction.ASC)
                .on(FECHA, Sort.Direction.DESC)
                .on("_id", Sort.Direction.ASC));
        mongo.indexOps(HabilitacionOpinionDocument.class).createIndex(new Index()
                .named("habilitacion_unica")
                .on(HabilitacionOpinionDocument.TURISTA_ID, Sort.Direction.ASC)
                .on(HabilitacionOpinionDocument.OFERTA_ID, Sort.Direction.ASC)
                .unique());
    }
}
