package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.FotoSubidaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ZonaMongoMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.stereotype.Component;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.PRESTADOR_ID;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument.TIPO;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Crea los índices del catálogo al arrancar (DC-17). Crear un índice que ya existe con la misma definición no hace
 * nada, así que todas las réplicas pueden hacerlo.
 *
 * <ul>
 *   <li>{@code busqueda_tipo_zona_precio}: la clave de QAS-07, para la búsqueda que filtra por tipo y zona.</li>
 *   <li>{@code busqueda_precio}: para la búsqueda que no filtra por tipo; ordena por precio sin recorrer todo.</li>
 *   <li>{@code ofertas_del_prestador}: para el {@code updateMany} del RNT y para listar las ofertas del prestador
 *       en orden de id.</li>
 *   <li>{@code en_moderacion}: parcial, solo con las ofertas en moderación, para listarlas de la más antigua a la
 *       más reciente (RF-79, DC-38).</li>
 *   <li>{@code perfil_por_prestador}: único, un perfil por prestador.</li>
 *   <li>{@code fotos_publicadas}, {@code fotos_pendientes} y {@code fotos_del_perfil}: para saber si una foto está en
 *       uso antes de borrarla (DC-47).</li>
 *   <li>{@code fotos_por_ultimo_uso}: para que la limpieza recorra las fotos sin uso de la más antigua a la más
 *       reciente (DC-47).</li>
 *   <li>{@code zona_por_nombre}: único, para que dos zonas no se llamen igual sin distinguir mayúsculas ni tildes
 *       (DC-49).</li>
 * </ul>
 *
 * <p>Los dos índices de búsqueda son parciales: solo guardan las ofertas visibles (ReglaVisibilidad).
 */
@Component
@Profile("mongo")
public class IndicesMongo implements InitializingBean {

    private final MongoTemplate mongo;

    public IndicesMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void afterPropertiesSet() {
        crear();
    }

    public void crear() {
        IndexOperations ofertas = mongo.indexOps(OfertaDocument.class);
        PartialIndexFilter soloVisibles = PartialIndexFilter.of(BusquedaOfertasMongo.visibles());
        ofertas.createIndex(new Index()
                .named("busqueda_tipo_zona_precio")
                .on(TIPO, Sort.Direction.ASC)
                .on(BusquedaOfertasMongo.ZONA_ID, Sort.Direction.ASC)
                .on(BusquedaOfertasMongo.PRECIO, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .partial(soloVisibles));
        ofertas.createIndex(new Index()
                .named("busqueda_precio")
                .on(BusquedaOfertasMongo.PRECIO, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .partial(soloVisibles));
        ofertas.createIndex(new Index()
                .named("ofertas_del_prestador")
                .on(PRESTADOR_ID, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC));
        ofertas.createIndex(new Index()
                .named("en_moderacion")
                .on(OfertaMongoAdapter.PENDIENTE_ENVIADA_EN, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .partial(PartialIndexFilter.of(OfertaMongoAdapter.enModeracion())));
        ofertas.createIndex(new Index()
                .named("fotos_publicadas")
                .on(OfertaMongoAdapter.PUBLICADA_FOTOS, Sort.Direction.ASC));
        ofertas.createIndex(new Index()
                .named("fotos_pendientes")
                .on(OfertaMongoAdapter.PENDIENTE_FOTOS, Sort.Direction.ASC));
        IndexOperations perfiles = mongo.indexOps(PerfilDocument.class);
        perfiles.createIndex(new Index()
                .named("perfil_por_prestador")
                .on(PerfilDocument.PRESTADOR_ID, Sort.Direction.ASC)
                .unique());
        perfiles.createIndex(new Index()
                .named("fotos_del_perfil")
                .on(PerfilDocument.FOTOS, Sort.Direction.ASC));
        mongo.indexOps(FotoSubidaDocument.class).createIndex(new Index()
                .named("fotos_por_ultimo_uso")
                .on(FotoSubidaDocument.ULTIMO_USO, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC));
        completarClaveDeLasZonas();
        mongo.indexOps(ZonaDocument.class).createIndex(new Index()
                .named("zona_por_nombre")
                .on(ZonaDocument.NOMBRE_CLAVE, Sort.Direction.ASC)
                .unique());
    }

    /**
     * Las zonas guardadas antes de DC-49 no tienen {@code nombreClave}. Sin ella, el índice único vería todas con la
     * clave vacía y no se podría crear; por eso se completa antes. Las réplicas pueden hacerlo a la vez: escriben lo
     * mismo.
     */
    private void completarClaveDeLasZonas() {
        mongo.find(query(where(ZonaDocument.NOMBRE_CLAVE).exists(false)), ZonaDocument.class)
                .forEach(zona -> mongo.save(ZonaMongoMapper.aDocumento(ZonaMongoMapper.aDominio(zona))));
    }
}
