package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.LAURA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_LAURA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.SNORKEL_PLAYA_CRISTAL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * La carga de las opiniones de demostración contra un MongoDB real: deja lo mismo que los adaptadores en memoria, no
 * toca una base con datos y aguanta que las réplicas arranquen a la vez.
 */
class CargaDemoOpinionesMongoIT extends BasePersistenciaOpinionesMongo {

    @Autowired
    private OpinionMongoAdapter opiniones;

    @Autowired
    private HabilitacionOpinionMongoAdapter habilitaciones;

    @Autowired
    private IndicesOpinionesMongo indices;

    @BeforeEach
    void vaciarLaBase() {
        mongo.remove(new Query(), OpinionDocument.class);
        mongo.remove(new Query(), HabilitacionOpinionDocument.class);
        indices.crear();
    }

    @Test
    @DisplayName("con la base vacía carga las opiniones y las habilitaciones de la demostración")
    void cargaLosDatos() {
        new CargaDemoOpinionesMongo(mongo).cargar();

        assertThat(mongo.count(new Query(), OpinionDocument.class)).isEqualTo(DatosOpiniones.opiniones().size());
        assertThat(mongo.count(new Query(), HabilitacionOpinionDocument.class))
                .isEqualTo(DatosOpiniones.habilitaciones().size());
        assertThat(opiniones.buscarPorId(OPINION_LAURA_CABO_SAN_JUAN)).hasValueSatisfying(opinion -> {
            assertThat(opinion.estaAprobada()).isTrue();
            assertThat(opinion.revision()).isEqualTo(Opinion.REVISION_NUEVA + 1);
        });
        assertThat(habilitaciones.existe(LAURA, SNORKEL_PLAYA_CRISTAL)).isTrue();
    }

    @Test
    @DisplayName("cargar dos veces no duplica nada, y una colección con datos no se carga")
    void esIdempotente() {
        CargaDemoOpinionesMongo carga = new CargaDemoOpinionesMongo(mongo);
        carga.cargar();
        opiniones.eliminarDelTurista(LAURA);

        carga.cargar();

        assertThat(mongo.count(new Query(), OpinionDocument.class))
                .as("la segunda carga no repone lo que se borró después de la primera")
                .isEqualTo(DatosOpiniones.opiniones().size() - 1);
        assertThat(mongo.count(new Query(), HabilitacionOpinionDocument.class))
                .isEqualTo(DatosOpiniones.habilitaciones().size());
    }

    @Test
    @DisplayName("dos réplicas que cargan a la vez dejan una sola copia de cada documento, sin errores")
    void dosReplicasALaVez() throws Exception {
        Callable<Void> replica = () -> {
            new CargaDemoOpinionesMongo(mongo).cargar();
            return null;
        };
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            for (Future<Void> resultado : hilos.invokeAll(List.of(replica, replica))) {
                resultado.get();
            }
        }

        assertThat(mongo.count(new Query(), OpinionDocument.class)).isEqualTo(DatosOpiniones.opiniones().size());
        assertThat(mongo.count(new Query(), HabilitacionOpinionDocument.class))
                .isEqualTo(DatosOpiniones.habilitaciones().size());
    }
}
