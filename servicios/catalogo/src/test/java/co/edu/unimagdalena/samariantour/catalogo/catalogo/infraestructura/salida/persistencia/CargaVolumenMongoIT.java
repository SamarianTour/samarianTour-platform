package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosDeCarga;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
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

import static org.assertj.core.api.Assertions.assertThat;

/** La carga del catálogo sintético contra un MongoDB real (QAS-07, DC-48). */
class CargaVolumenMongoIT extends BasePersistenciaMongo {

    private static final int CANTIDAD = 100;

    @Autowired
    private OfertaMongoAdapter ofertas;

    @Autowired
    private IndicesMongo indices;

    @BeforeEach
    void vaciarLaBase() {
        for (Class<?> coleccion : List.of(ZonaDocument.class, PrestadorDocument.class, PerfilDocument.class,
                OfertaDocument.class)) {
            mongo.remove(new Query(), coleccion);
        }
        indices.crear();
    }

    @Test
    @DisplayName("después de la demo, agrega las ofertas sintéticas y sus prestadores sin tocar las de la demo")
    void cargaDespuesDeLaDemo() {
        new CargaDemoMongo(mongo).cargar();

        int insertadas = new CargaVolumenMongo(mongo, CANTIDAD).cargar();

        assertThat(insertadas).isEqualTo(CANTIDAD);
        assertThat(mongo.count(new Query(), OfertaDocument.class))
                .isEqualTo(DatosSantaMarta.ofertas().size() + CANTIDAD);
        assertThat(mongo.count(new Query(), PrestadorDocument.class)).isEqualTo(
                DatosSantaMarta.prestadores().size() + DatosDeCarga.prestadores(CANTIDAD).size());
        Oferta esperada = DatosDeCarga.ofertas(CANTIDAD).getFirst();
        assertThat(ofertas.buscarPorId(esperada.id())).get().usingRecursiveComparison().ignoringFields("revision")
                .isEqualTo(esperada);
    }

    @Test
    @DisplayName("si ya están todas, no inserta nada")
    void esIdempotente() {
        CargaVolumenMongo carga = new CargaVolumenMongo(mongo, CANTIDAD);
        carga.cargar();

        assertThat(carga.cargar()).isZero();
        assertThat(mongo.count(new Query(), OfertaDocument.class)).isEqualTo(CANTIDAD);
    }

    @Test
    @DisplayName("dos réplicas que cargan a la vez dejan una sola copia de cada oferta, sin errores")
    void dosReplicasALaVez() throws Exception {
        Callable<Integer> replica = () -> new CargaVolumenMongo(mongo, CANTIDAD).cargar();
        int total = 0;
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            for (Future<Integer> resultado : hilos.invokeAll(List.of(replica, replica))) {
                total += resultado.get();
            }
        }

        assertThat(total).isEqualTo(CANTIDAD);
        assertThat(mongo.count(new Query(), OfertaDocument.class)).isEqualTo(CANTIDAD);
        assertThat(mongo.count(new Query(), PrestadorDocument.class))
                .isEqualTo(DatosDeCarga.prestadores(CANTIDAD).size());
    }
}
