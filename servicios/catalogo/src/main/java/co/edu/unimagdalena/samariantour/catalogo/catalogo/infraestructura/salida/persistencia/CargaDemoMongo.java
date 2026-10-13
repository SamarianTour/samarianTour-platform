package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.OfertaMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PerfilMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PrestadorMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ZonaMongoMapper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Carga los datos de demostración de Santa Marta en MongoDB al arrancar, con los perfiles {@code mongo} y
 * {@code demo}. Una colección solo se carga si está vacía, así que no toca una base que ya tiene datos.
 *
 * <p>Las réplicas arrancan a la vez y pueden ver vacía la misma colección. Por eso cada documento se inserta por
 * separado, con su identificador fijo, y un duplicado se ignora: ya lo insertó la otra réplica. Así la carga queda
 * idempotente sin coordinación entre réplicas.
 */
@Component
@Profile("mongo & demo")
public class CargaDemoMongo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaDemoMongo.class);

    private final MongoTemplate mongo;

    public CargaDemoMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        cargar();
    }

    public void cargar() {
        int zonas = cargar(ZonaDocument.class,
                DatosSantaMarta.zonas().stream().map(ZonaMongoMapper::aDocumento).toList());
        int prestadores = cargar(PrestadorDocument.class,
                DatosSantaMarta.prestadores().stream().map(PrestadorMongoMapper::aDocumento).toList());
        int perfiles = cargar(PerfilDocument.class, perfiles());
        // Una oferta nueva queda con la primera revisión guardada, igual que al insertarla el adaptador (ADR-05).
        int ofertas = cargar(OfertaDocument.class, DatosSantaMarta.ofertas().stream()
                .map(oferta -> OfertaMongoMapper.aDocumento(oferta, Oferta.REVISION_NUEVA + 1))
                .toList());
        LOG.info("Datos de demostración insertados por esta réplica: {} zonas, {} prestadores, {} perfiles, "
                + "{} ofertas", zonas, prestadores, perfiles, ofertas);
    }

    /** @return cuántos documentos insertó esta réplica */
    private <T> int cargar(Class<T> tipo, List<T> documentos) {
        if (mongo.count(new Query(), tipo) > 0) {
            return 0;
        }
        int insertados = 0;
        for (T documento : documentos) {
            if (insertar(documento)) {
                insertados++;
            }
        }
        return insertados;
    }

    /** @return false si el documento ya existía porque lo insertó otra réplica */
    private boolean insertar(Object documento) {
        try {
            mongo.insert(documento);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** El perfil no tiene identificador en el dominio: su {@code _id} fijo sale de su posición en los datos. */
    private static List<PerfilDocument> perfiles() {
        List<PerfilDocument> documentos = new ArrayList<>();
        for (PerfilPublico perfil : DatosSantaMarta.perfiles()) {
            documentos.add(PerfilMongoMapper.aDocumento(idDePerfil(documentos.size() + 1), perfil));
        }
        return documentos;
    }

    static ObjectId idDePerfil(int posicion) {
        return new ObjectId("c%023x".formatted(posicion));
    }
}
