package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ZonaMongoMapper;
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

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TAYRONA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * La carga de los datos de demostración contra un MongoDB real: deja lo mismo que los adaptadores en memoria,
 * no toca una base con datos y aguanta que las réplicas arranquen a la vez.
 */
class CargaDemoMongoIT extends BasePersistenciaMongo {

    @Autowired
    private OfertaMongoAdapter ofertas;

    @Autowired
    private ZonaMongoAdapter zonas;

    @Autowired
    private PerfilMongoAdapter perfiles;

    @Autowired
    private PrestadorMongoAdapter prestadores;

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
    @DisplayName("con la base vacía carga las zonas, los prestadores, los perfiles y las ofertas de la demostración")
    void cargaLosDatos() {
        new CargaDemoMongo(mongo).cargar();

        assertThat(mongo.count(new Query(), OfertaDocument.class)).isEqualTo(DatosSantaMarta.ofertas().size());
        for (Zona zona : DatosSantaMarta.zonas()) {
            assertThat(zonas.buscarPorId(zona.id())).contains(zona);
        }
        for (PrestadorCatalogo prestador : DatosSantaMarta.prestadores()) {
            assertThat(prestadores.buscarPorId(prestador.prestadorId())).hasValueSatisfying(guardado -> {
                assertThat(guardado.rntActivo()).isEqualTo(prestador.rntActivo());
                assertThat(guardado.version()).isEqualTo(prestador.version());
            });
        }
        for (PerfilPublico perfil : DatosSantaMarta.perfiles()) {
            assertThat(perfiles.buscarPorPrestador(perfil.prestadorId())).contains(perfil);
        }
        assertThat(perfiles.buscarPorPrestador(AGENCIA)).map(PerfilPublico::nombreComercial)
                .contains("Mar y Selva Expediciones");
        assertThat(ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN)).hasValueSatisfying(caminata -> {
            assertThat(caminata.estaVisible()).isTrue();
            assertThat(caminata.revision()).isEqualTo(Oferta.REVISION_NUEVA + 1);
            assertThat(caminata.publicada().orElseThrow().contenido().ubicacion()).isNotNull();
            assertThat(caminata.pendiente()).isPresent();
        });
    }

    @Test
    @DisplayName("cargar dos veces no duplica ni cambia nada")
    void esIdempotente() {
        CargaDemoMongo carga = new CargaDemoMongo(mongo);
        carga.cargar();
        Oferta caminata = ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();
        caminata.retirar(AGENCIA);
        ofertas.guardar(caminata);

        carga.cargar();

        assertThat(mongo.count(new Query(), OfertaDocument.class)).isEqualTo(DatosSantaMarta.ofertas().size());
        assertThat(mongo.count(new Query(), PerfilDocument.class)).isEqualTo(DatosSantaMarta.perfiles().size());
        assertThat(ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow().retirada())
                .as("la segunda carga no pisa lo que cambió después de la primera").isTrue();
    }

    @Test
    @DisplayName("una colección que ya tiene datos no se carga")
    void noTocaUnaBaseConDatos() {
        Zona tayrona = DatosSantaMarta.zonas().stream().filter(zona -> zona.id().equals(TAYRONA)).findFirst()
                .orElseThrow();
        mongo.insert(ZonaMongoMapper.aDocumento(tayrona));

        new CargaDemoMongo(mongo).cargar();

        assertThat(mongo.count(new Query(), ZonaDocument.class)).isEqualTo(1);
        assertThat(mongo.count(new Query(), OfertaDocument.class)).isEqualTo(DatosSantaMarta.ofertas().size());
    }

    @Test
    @DisplayName("dos réplicas que cargan a la vez dejan una sola copia de cada documento, sin errores")
    void dosReplicasALaVez() throws Exception {
        Callable<Void> replica = () -> {
            new CargaDemoMongo(mongo).cargar();
            return null;
        };
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            for (Future<Void> resultado : hilos.invokeAll(List.of(replica, replica))) {
                resultado.get();
            }
        }

        assertThat(mongo.count(new Query(), ZonaDocument.class)).isEqualTo(DatosSantaMarta.zonas().size());
        assertThat(mongo.count(new Query(), PrestadorDocument.class)).isEqualTo(DatosSantaMarta.prestadores().size());
        assertThat(mongo.count(new Query(), PerfilDocument.class)).isEqualTo(DatosSantaMarta.perfiles().size());
        assertThat(mongo.count(new Query(), OfertaDocument.class)).isEqualTo(DatosSantaMarta.ofertas().size());
    }
}
