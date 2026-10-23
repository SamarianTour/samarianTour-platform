package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.programada;

import co.edu.unimagdalena.samariantour.catalogo.IntegracionMongoBase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.LimpiarFotosSinUsoUseCase;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * La limpieza de fotos con el perfil {@code mongo}: el candado de ShedLock vive en MongoDB, así que la comparten todas
 * las réplicas (DC-47).
 */
@SpringBootTest(properties = "catalogo.fotos.limpieza.cron=-")
@ActiveProfiles("mongo")
class LimpiezaFotosProgramadaIT extends IntegracionMongoBase {

    @Autowired
    private LimpiezaFotosProgramada tarea;

    @Autowired
    private MongoTemplate mongo;

    @MockitoBean
    private LimpiarFotosSinUsoUseCase limpieza;

    @Test
    @DisplayName("mientras el candado está tomado, otra ejecución se salta la limpieza")
    void unaSolaEjecucion() {
        tarea.limpiar();
        tarea.limpiar();

        verify(limpieza, times(1)).limpiar();
        Document candado = mongo.getCollection("shedLock").find(eq("_id", "limpiar-fotos-sin-uso")).first();
        assertThat(candado).as("el candado quedó en MongoDB").isNotNull();
        assertThat(candado.getDate("lockUntil")).as("dura al menos 5 minutos").isInTheFuture();
    }
}
