package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
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

/**
 * Crea los índices del catálogo al arrancar (DC-17). Crear un índice que ya existe con la misma definición no hace
 * nada, así que todas las réplicas pueden hacerlo.
 *
 * <ul>
 *   <li>{@code busqueda_tipo_zona_precio}: la clave de QAS-07, para la búsqueda que filtra por tipo y zona.</li>
 *   <li>{@code busqueda_precio}: para la búsqueda que no filtra por tipo; ordena por precio sin recorrer todo.</li>
 *   <li>{@code ofertas_del_prestador}: para el {@code updateMany} del RNT y para listar las ofertas del prestador
 *       en orden de id.</li>
 *   <li>{@code perfil_por_prestador}: único, un perfil por prestador.</li>
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
        mongo.indexOps(PerfilDocument.class).createIndex(new Index()
                .named("perfil_por_prestador")
                .on(PerfilDocument.PRESTADOR_ID, Sort.Direction.ASC)
                .unique());
    }
}
