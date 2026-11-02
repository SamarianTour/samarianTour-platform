package co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AlertaSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AparicionesPrestador;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertasElegibles;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RevisionSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RevisionSesgo.Resultado;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.AlertaSesgoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.ExposicionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.TransaccionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** La revisión diaria del sesgo con dobles en memoria (RET-02, DN-43). */
class EvaluarIndiceExposicionServiceTest {

    /** Lunes 2 de noviembre de 2026 a las 9:00 en Colombia. */
    private static final Instant AHORA = Instant.parse("2026-11-02T14:00:00Z");
    private static final LocalDate HOY = LocalDate.parse("2026-11-02");
    private static final UUID MAR_Y_SELVA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID KAYAK = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private long recomendaciones = 300;
    private List<AparicionesPrestador> apariciones = List.of(new AparicionesPrestador(MAR_Y_SELVA, 30),
            new AparicionesPrestador(KAYAK, 70));
    private List<OfertasElegibles> elegibles = List.of(new OfertasElegibles(MAR_Y_SELVA, 10),
            new OfertasElegibles(KAYAK, 90));
    private boolean catalogoCaido;
    private final List<Instant> borrados = new ArrayList<>();
    private final Map<UUID, AlertaSesgo> guardadas = new LinkedHashMap<>();
    private final Set<LocalDate> revisados = new HashSet<>();

    private final ExposicionRepositoryPort exposiciones = new ExposicionRepositoryPort() {
        @Override
        public void guardar(RegistroExposicion registro) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long recomendacionesDesde(Instant desde) {
            return recomendaciones;
        }

        @Override
        public List<AparicionesPrestador> aparicionesDesde(Instant desde) {
            return apariciones;
        }

        @Override
        public int borrarAnteriores(Instant limite) {
            borrados.add(limite);
            return 0;
        }
    };

    private final AlertaSesgoRepositoryPort alertas = new AlertaSesgoRepositoryPort() {
        @Override
        public List<AlertaSesgo> abiertas() {
            return guardadas.values().stream().filter(alerta -> alerta.cerradaEn() == null)
                    .sorted(Comparator.comparing(AlertaSesgo::indice).reversed()).toList();
        }

        @Override
        public void guardar(AlertaSesgo alerta) {
            guardadas.put(alerta.id(), alerta);
        }

        @Override
        public boolean revisado(LocalDate dia) {
            return revisados.contains(dia);
        }

        @Override
        public boolean reservarRevision(LocalDate dia, Instant ahora) {
            return revisados.add(dia);
        }
    };

    private final TransaccionPort transaccion = new TransaccionPort() {
        @Override
        public <T> T ejecutar(Supplier<T> operacion) {
            return operacion.get();
        }
    };

    private final EvaluarIndiceExposicionService servicio = new EvaluarIndiceExposicionService(exposiciones, alertas,
            () -> {
                if (catalogoCaido) {
                    throw new CatalogoNoDisponibleException("no respondió");
                }
                return elegibles;
            }, transaccion, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    @DisplayName("abre una alerta para quien supera 1,5, borra lo de más de 90 días y no repite el mismo día")
    void abrir() {
        RevisionSesgo revision = servicio.evaluar();

        assertThat(revision).isEqualTo(new RevisionSesgo(HOY, Resultado.EVALUADA, 1, 0, 0));
        assertThat(alertas.abiertas()).singleElement().satisfies(alerta -> {
            assertThat(alerta.prestadorId()).isEqualTo(MAR_Y_SELVA);
            assertThat(alerta.indice()).isEqualByComparingTo("3.00");
            assertThat(alerta.recomendaciones()).isEqualTo(300);
            assertThat(alerta.abiertaEn()).isEqualTo(AHORA);
        });
        assertThat(borrados).containsExactly(AHORA.minus(Duration.ofDays(90)));
        assertThat(servicio.evaluar().resultado()).isEqualTo(Resultado.YA_REVISADA);
    }

    @Test
    @DisplayName("al día siguiente la actualiza si sigue por encima, y la cierra cuando baja")
    void actualizarYCerrar() {
        servicio.evaluar();
        UUID id = alertas.abiertas().getFirst().id();

        revisados.clear();
        apariciones = List.of(new AparicionesPrestador(MAR_Y_SELVA, 20), new AparicionesPrestador(KAYAK, 80));
        assertThat(servicio.evaluar().actualizadas()).isEqualTo(1);
        assertThat(guardadas.get(id).indice()).isEqualByComparingTo(new BigDecimal("2.00"));

        revisados.clear();
        apariciones = List.of(new AparicionesPrestador(MAR_Y_SELVA, 10), new AparicionesPrestador(KAYAK, 90));
        assertThat(servicio.evaluar().cerradas()).isEqualTo(1);
        assertThat(guardadas.get(id).cerradaEn()).isEqualTo(AHORA);
        assertThat(alertas.abiertas()).isEmpty();
    }

    @Test
    @DisplayName("con menos de 200 recomendaciones no mide ni toca las alertas, pero el día queda revisado")
    void pocasRecomendaciones() {
        recomendaciones = 199;

        assertThat(servicio.evaluar().resultado()).isEqualTo(Resultado.POCAS_RECOMENDACIONES);
        assertThat(guardadas).isEmpty();
        assertThat(revisados).containsExactly(HOY);
    }

    @Test
    @DisplayName("si el catálogo no responde, no reserva el día: se intenta a la hora siguiente")
    void catalogoCaido() {
        catalogoCaido = true;

        assertThat(servicio.evaluar().resultado()).isEqualTo(Resultado.CATALOGO_NO_DISPONIBLE);
        assertThat(revisados).isEmpty();

        catalogoCaido = false;
        assertThat(servicio.evaluar().resultado()).isEqualTo(Resultado.EVALUADA);
    }
}
