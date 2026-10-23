package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.mapper.OpinionMongoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga las opiniones y habilitaciones de demostración en MongoDB al arrancar, con los perfiles {@code mongo} y
 * {@code demo}, con las mismas reglas que {@code CargaDemoMongo} del catálogo (DC-27): una colección solo se carga si
 * está vacía, y cada documento se inserta por separado ignorando los duplicados, para que dos réplicas puedan cargar
 * a la vez. Es del módulo porque el catálogo no puede depender de opiniones (DC-40).
 */
@Component
@Profile("mongo & demo")
public class CargaDemoOpinionesMongo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaDemoOpinionesMongo.class);

    private final MongoTemplate mongo;

    public CargaDemoOpinionesMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        cargar();
    }

    public void cargar() {
        // Una opinión nueva queda con la primera revisión guardada, igual que al insertarla el adaptador (ADR-05).
        int opiniones = cargar(OpinionDocument.class, DatosOpiniones.opiniones().stream()
                .map(opinion -> OpinionMongoMapper.aDocumento(opinion, Opinion.REVISION_NUEVA + 1))
                .toList());
        int habilitaciones = cargar(HabilitacionOpinionDocument.class, DatosOpiniones.habilitaciones().stream()
                .map(OpinionMongoMapper::aDocumento)
                .toList());
        LOG.info("Datos de demostración de opiniones insertados por esta réplica: {} opiniones, {} habilitaciones",
                opiniones, habilitaciones);
    }

    /** @return cuántos documentos insertó esta réplica */
    private <T> int cargar(Class<T> tipo, List<T> documentos) {
        if (mongo.count(new Query(), tipo) > 0) {
            return 0;
        }
        int insertados = 0;
        for (T documento : documentos) {
            try {
                mongo.insert(documento);
                insertados++;
            } catch (DuplicateKeyException e) {
                // Ya lo insertó otra réplica.
            }
        }
        return insertados;
    }
}
