package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosDeCarga;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.OfertaMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PrestadorMongoMapper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga el catálogo sintético de {@link DatosDeCarga} para la prueba de carga (QAS-07, DC-48), con los perfiles
 * {@code mongo} y {@code carga}. Se usa junto con {@code demo}, que trae las zonas y las fotos, y corre después de
 * la carga de demostración: esa solo carga las ofertas si la colección está vacía.
 *
 * <p>Como la carga de demostración, es idempotente sin coordinar a las réplicas: los identificadores son fijos, se
 * insertan sin orden y un duplicado se ignora porque ya lo insertó otra réplica. Si ya están todas, no hace nada.
 */
@Component
@Profile("mongo & carga")
@Order(Ordered.LOWEST_PRECEDENCE)
public class CargaVolumenMongo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaVolumenMongo.class);

    private final MongoTemplate mongo;
    private final int cantidad;

    public CargaVolumenMongo(MongoTemplate mongo, @Value("${catalogo.carga.ofertas}") int cantidad) {
        this.mongo = mongo;
        this.cantidad = cantidad;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        cargar();
    }

    /** @return cuántas ofertas insertó esta réplica */
    public int cargar() {
        if (yaCargadas() == cantidad) {
            LOG.info("El catálogo de carga ya tiene sus {} ofertas", cantidad);
            return 0;
        }
        insertar(PrestadorDocument.class, DatosDeCarga.prestadores(cantidad).stream()
                .map(PrestadorMongoMapper::aDocumento).toList());
        // Una oferta nueva queda con la primera revisión guardada, igual que al insertarla el adaptador (ADR-05).
        int ofertas = insertar(OfertaDocument.class, DatosDeCarga.ofertas(cantidad).stream()
                .map(oferta -> OfertaMongoMapper.aDocumento(oferta, Oferta.REVISION_NUEVA + 1)).toList());
        LOG.info("Ofertas del catálogo de carga insertadas por esta réplica: {} de {}", ofertas, cantidad);
        return ofertas;
    }

    private long yaCargadas() {
        return mongo.count(new Query(Criteria.where("_id")
                .gte(new ObjectId(DatosDeCarga.idDeOferta(1)))
                .lte(new ObjectId(DatosDeCarga.idDeOferta(cantidad)))), OfertaDocument.class);
    }

    /** @return cuántos documentos insertó; los que ya existían no cuentan */
    private int insertar(Class<?> tipo, List<?> documentos) {
        try {
            return mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, tipo).insert(documentos).execute()
                    .getInsertedCount();
        } catch (BulkOperationException e) {
            return e.getResult().getInsertedCount();
        }
    }
}
