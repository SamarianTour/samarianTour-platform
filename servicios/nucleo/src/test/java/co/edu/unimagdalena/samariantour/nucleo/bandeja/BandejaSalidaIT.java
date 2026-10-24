package co.edu.unimagdalena.samariantour.nucleo.bandeja;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaSalidaApi;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.DestinoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.EstadoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.PublicarAvisosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La bandeja de salida contra PostgreSQL y un catálogo simulado (DN-06): se encola en la transacción de quien origina
 * el aviso, se publica hasta que el catálogo lo recibe y dos réplicas nunca toman el mismo aviso.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
class BandejaSalidaIT extends IntegracionPostgresBase {

    private static final CatalogoSimulado CATALOGO = new CatalogoSimulado();
    private static final String TURISTA = "80000000-0000-0000-0000-000000000008";

    @Autowired
    private BandejaSalidaApi api;

    @Autowired
    private PublicarAvisosUseCase publicar;

    @Autowired
    private BandejaSalidaRepositoryPort bandeja;

    @Autowired
    private TransactionTemplate transaccion;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void catalogoSimulado(DynamicPropertyRegistry registro) {
        registro.add("nucleo.catalogo.url", CATALOGO::url);
    }

    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM bandeja_salida");
        CATALOGO.reiniciar();
    }

    @AfterAll
    static void apagar() {
        CATALOGO.close();
    }

    @Test
    @DisplayName("encola el aviso en JSON dentro de la transacción; repetir el identificador no lo duplica")
    void encolar() {
        assertThat(enTransaccion("sup-1", supresion("sup-1"))).isTrue();
        assertThat(enTransaccion("sup-1", supresion("sup-1"))).isFalse();

        AvisoSaliente aviso = bandeja.buscar("sup-1").orElseThrow();
        assertThat(aviso.estado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(aviso.cuerpo()).contains("\"tipo\": \"SUPRESION_TURISTA\"").contains(TURISTA);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bandeja_salida", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("si la transacción del cambio se deshace, el aviso tampoco queda; sin transacción, no se encola")
    void mismaTransaccion() {
        transaccion.executeWithoutResult(estado -> {
            api.encolar("sup-1", DestinoAviso.OPINIONES, supresion("sup-1"));
            estado.setRollbackOnly();
        });
        assertThat(bandeja.buscar("sup-1")).isEmpty();

        assertThatThrownBy(() -> api.encolar("sup-2", DestinoAviso.OPINIONES, supresion("sup-2")))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(bandeja.buscar("sup-2")).isEmpty();
    }

    @Test
    @DisplayName("publica cada aviso en la entrada de su destino y lo marca enviado")
    void publicar() {
        enTransaccion("sup-1", supresion("sup-1"));
        Map<String, Object> habilitacion = new LinkedHashMap<>();
        habilitacion.put("idAviso", "hab-1");
        habilitacion.put("tipo", "HABILITACION");
        transaccion.executeWithoutResult(estado -> api.encolar("hab-1", DestinoAviso.OFERTAS, habilitacion));

        assertThat(publicar.publicarLote()).isEqualTo(2);

        assertThat(CATALOGO.recibidas()).extracting(CatalogoSimulado.Peticion::ruta)
                .containsExactlyInAnyOrder("/internal/opiniones/avisos", "/internal/avisos");
        // PostgreSQL devuelve el JSONB con su propio formato: mismo contenido, con espacios y otro orden de claves.
        assertThat(CATALOGO.recibidas()).anySatisfy(peticion -> assertThat(peticion.cuerpo())
                .contains("\"idAviso\": \"hab-1\"").contains("\"tipo\": \"HABILITACION\""));
        assertThat(bandeja.buscar("sup-1").orElseThrow().estado()).isEqualTo(EstadoAviso.ENVIADO);
        assertThat(bandeja.buscar("hab-1").orElseThrow().enviadoEn()).isNotNull();
        assertThat(publicar.publicarLote()).isZero();
    }

    @Test
    @DisplayName("si el catálogo falla, el aviso queda pendiente para más tarde; si lo rechaza, queda rechazado")
    void fallosYRechazos() {
        enTransaccion("sup-1", supresion("sup-1"));
        CATALOGO.responder(503);
        publicar.publicarLote();

        AvisoSaliente pendiente = bandeja.buscar("sup-1").orElseThrow();
        assertThat(pendiente.estado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(pendiente.intentos()).isEqualTo(1);
        assertThat(pendiente.proximoIntento()).isAfter(Instant.now());
        assertThat(pendiente.ultimoError()).startsWith("503");
        assertThat(publicar.publicarLote()).as("todavía no toca reintentar").isZero();

        enTransaccion("sup-2", supresion("sup-2"));
        CATALOGO.responder(400);
        publicar.publicarLote();
        assertThat(bandeja.buscar("sup-2").orElseThrow().estado()).isEqualTo(EstadoAviso.RECHAZADO);
    }

    @Test
    @DisplayName("dos réplicas que publican a la vez toman avisos distintos (FOR UPDATE SKIP LOCKED)")
    void dosReplicasSinRepetir() throws Exception {
        for (int i = 1; i <= 15; i++) {
            enTransaccion("sup-" + i, supresion("sup-" + i));
        }
        CountDownLatch primeraTomo = new CountDownLatch(1);
        CountDownLatch segundaTomo = new CountDownLatch(1);

        CompletableFuture<List<String>> primera = CompletableFuture.supplyAsync(() -> transaccion.execute(estado -> {
            List<String> ids = idsDe(bandeja.tomarPendientes(Instant.now(), 10));
            primeraTomo.countDown();
            esperar(segundaTomo);
            return ids;
        }));
        esperar(primeraTomo);
        List<String> segunda = transaccion.execute(estado -> idsDe(bandeja.tomarPendientes(Instant.now(), 10)));
        segundaTomo.countDown();

        Set<String> todas = new HashSet<>(primera.get(10, TimeUnit.SECONDS));
        assertThat(todas).hasSize(10);
        assertThat(segunda).hasSize(5).doesNotContainAnyElementsOf(todas);
    }

    private boolean enTransaccion(String idAviso, Object cuerpo) {
        return Boolean.TRUE.equals(transaccion.execute(
                estado -> api.encolar(idAviso, DestinoAviso.OPINIONES, cuerpo)));
    }

    /** El aviso que Cuentas enviará al suprimir los datos de un turista (DC-45 del catálogo). */
    private static Map<String, Object> supresion(String idAviso) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idAviso", idAviso);
        cuerpo.put("tipo", "SUPRESION_TURISTA");
        cuerpo.put("turistaId", TURISTA);
        return cuerpo;
    }

    private static List<String> idsDe(List<AvisoSaliente> avisos) {
        return avisos.stream().map(AvisoSaliente::idAviso).toList();
    }

    private static void esperar(CountDownLatch senal) {
        try {
            assertThat(senal.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
