package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.PlazoVencidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SinTiempoParaConfirmarException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.TransicionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.atendida;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cabana;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cocinaCaribe;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.condiciones;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.confirmada;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.dosNoches;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.ATENDIDA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.CONFIRMADA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.DECLINADA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.NO_DISPONIBLE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.PAGADA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.PAGO_FALLIDO;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.PENDIENTE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.SIN_RESPUESTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.VENCIDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitudDisponibilidadTest {

    /** El diagrama de estados del SRS: lo que no está aquí es una transición inválida. */
    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> VALIDAS = Map.of(
            PENDIENTE, EnumSet.of(ATENDIDA, NO_DISPONIBLE, SIN_RESPUESTA),
            ATENDIDA, EnumSet.of(CONFIRMADA, DECLINADA, VENCIDA),
            CONFIRMADA, EnumSet.of(PAGADA, PAGO_FALLIDO, VENCIDA),
            PAGO_FALLIDO, EnumSet.of(PAGADA, PAGO_FALLIDO, VENCIDA));

    /** La ventana de una solicitud atendida a las 11:00 en Colombia: las 14:00. */
    private static final Instant FIN_VENTANA = AHORA.plus(Duration.ofHours(4));
    private static final CamposDelTipo A_LAS_SIETE = CamposDelTipo.actividad(SIETE);
    /** Lunes 26 a las 22:00 y martes 27 a las 21:00 en Colombia. */
    private static final Instant LUNES_22 = Instant.parse("2026-10-27T03:00:00Z");
    private static final Instant MARTES_21 = Instant.parse("2026-10-28T02:00:00Z");

    static Stream<Arguments> todasLasParejas() {
        return Arrays.stream(EstadoSolicitud.values())
                .flatMap(de -> Arrays.stream(EstadoSolicitud.values()).map(a -> Arguments.of(de, a)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("todasLasParejas")
    @DisplayName("solo las 12 transiciones del ciclo de vida son válidas; los estados terminales no salen")
    void matrizDeTransiciones(EstadoSolicitud de, EstadoSolicitud a) {
        boolean esperada = VALIDAS.getOrDefault(de, Set.of()).contains(a);

        assertThat(SolicitudDisponibilidad.esTransicionValida(de, a)).isEqualTo(esperada);
        if (de.esTerminal()) {
            assertThat(SolicitudDisponibilidad.esTransicionValida(de, a)).isFalse();
        }
    }

    @Test
    @DisplayName("terminales: NO_DISPONIBLE, SIN_RESPUESTA, DECLINADA, VENCIDA y PAGADA")
    void terminales() {
        assertThat(Arrays.stream(EstadoSolicitud.values()).filter(EstadoSolicitud::esTerminal))
                .containsExactlyInAnyOrder(NO_DISPONIBLE, SIN_RESPUESTA, DECLINADA, VENCIDA, PAGADA);
    }

    @Nested
    @DisplayName("crear (RF-30 a RF-32)")
    class Crear {

        @Test
        @DisplayName("queda Pendiente, con el operador del prestador, 24 h de plazo y la primera transición")
        void pendiente() {
            SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                    MIERCOLES, A_LAS_SIETE, 2, "  Con una niña  ", OPERADOR, AHORA);

            assertThat(solicitud.estado()).isEqualTo(PENDIENTE);
            assertThat(solicitud.operadorId()).isEqualTo(OPERADOR);
            assertThat(solicitud.venceEn()).isEqualTo(AHORA.plus(Duration.ofHours(24)));
            assertThat(solicitud.notaTurista()).isEqualTo("Con una niña");
            assertThat(solicitud.oferta().nombre()).isEqualTo("Caminata a Cabo San Juan del Guía");
            assertThat(solicitud.historial()).singleElement().satisfies(transicion -> {
                assertThat(transicion.de()).isNull();
                assertThat(transicion.a()).isEqualTo(PENDIENTE);
                assertThat(transicion.actor()).isEqualTo(ActorTransicion.TURISTA);
                assertThat(transicion.actorId()).isEqualTo(TURISTA);
            });
        }

        @Test
        @DisplayName("la fecha de inicio debe ser al menos 2 días calendario después del envío")
        void anticipacion() {
            assertThatThrownBy(() -> crear(caminata(), LocalDate.parse("2026-10-25"), A_LAS_SIETE, 2))
                    .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("fechaInicio:")
                    .hasMessageContaining("2026-10-26");
            assertThat(crear(caminata(), LocalDate.parse("2026-10-26"), A_LAS_SIETE, 2).estado())
                    .isEqualTo(PENDIENTE);
        }

        @Test
        @DisplayName("actividad y paquete exigen una de sus horas de salida y nada de los otros tipos")
        void actividad() {
            assertThatThrownBy(() -> crear(caminata(), MIERCOLES, CamposDelTipo.actividad(null), 2))
                    .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("horaSalida:");
            assertThatThrownBy(() -> crear(caminata(), MIERCOLES, CamposDelTipo.actividad(LocalTime.of(9, 0)), 2))
                    .isInstanceOf(DatoInvalidoException.class).hasMessageContaining("07:00");
            assertThatThrownBy(() -> crear(caminata(), MIERCOLES, new CamposDelTipo(SIETE, LocalTime.NOON, null,
                    null), 2)).isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("horaReserva:")
                    .hasMessageContaining("no aplica a una actividad");
            assertThatThrownBy(() -> crear(caminata(), MIERCOLES, new CamposDelTipo(SIETE, null, null, 1), 2))
                    .hasMessageStartingWith("numeroUnidades:");
            assertThat(crear(caminata(), MIERCOLES, A_LAS_SIETE, 2).horaSalida()).isEqualTo(SIETE);
        }

        @Test
        @DisplayName("alojamiento exige la fecha de salida, posterior a la de inicio, y las unidades; no lleva hora")
        void alojamiento() {
            assertThatThrownBy(() -> crear(cabana(), MIERCOLES, CamposDelTipo.alojamiento(null, 1), 2))
                    .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("fechaSalida:");
            assertThatThrownBy(() -> crear(cabana(), MIERCOLES, CamposDelTipo.alojamiento(MIERCOLES, 1), 2))
                    .hasMessageStartingWith("fechaSalida:").hasMessageContaining("posterior");
            assertThatThrownBy(() -> crear(cabana(), MIERCOLES,
                    CamposDelTipo.alojamiento(MIERCOLES.plusDays(1), null), 2))
                    .hasMessageStartingWith("numeroUnidades:").hasMessageContaining("obligatorio");
            assertThatThrownBy(() -> crear(cabana(), MIERCOLES,
                    CamposDelTipo.alojamiento(MIERCOLES.plusDays(1), 0), 2))
                    .hasMessageStartingWith("numeroUnidades:").hasMessageContaining("positivo");
            assertThatThrownBy(() -> crear(cabana(), MIERCOLES, A_LAS_SIETE, 2))
                    .hasMessageStartingWith("horaSalida:").hasMessageContaining("no aplica a un alojamiento");

            SolicitudDisponibilidad solicitud = crear(cabana(), MIERCOLES,
                    CamposDelTipo.alojamiento(MIERCOLES.plusDays(1), 2), 4);
            assertThat(solicitud.fechaSalida()).isEqualTo(MIERCOLES.plusDays(1));
            assertThat(solicitud.numeroUnidades()).isEqualTo(2);
            assertThat(solicitud.horaSalida()).isNull();
            assertThat(solicitud.horaReserva()).isNull();
        }

        @Test
        @DisplayName("restaurante exige la hora de la reserva y nada de los otros tipos")
        void restaurante() {
            assertThatThrownBy(() -> crear(cocinaCaribe(), MIERCOLES, CamposDelTipo.restaurante(null), 4))
                    .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("horaReserva:");
            assertThatThrownBy(() -> crear(cocinaCaribe(), MIERCOLES, A_LAS_SIETE, 4))
                    .hasMessageStartingWith("horaSalida:").hasMessageContaining("no aplica a un restaurante");
            assertThatThrownBy(() -> crear(cocinaCaribe(), MIERCOLES, dosNoches(MIERCOLES), 4))
                    .hasMessageStartingWith("fechaSalida:");

            assertThat(crear(cocinaCaribe(), MIERCOLES, CamposDelTipo.restaurante(LocalTime.of(19, 30)), 4)
                    .horaReserva()).isEqualTo(LocalTime.of(19, 30));
        }

        @Test
        @DisplayName("de 1 persona a la capacidad máxima de la oferta; la nota, hasta 500 caracteres")
        void personasYNota() {
            assertThatThrownBy(() -> crear(caminata(), MIERCOLES, A_LAS_SIETE, 0))
                    .hasMessageStartingWith("personas:");
            assertThatThrownBy(() -> crear(caminata(), MIERCOLES, A_LAS_SIETE, 13)).hasMessageContaining("12");
            assertThat(crear(caminata(), MIERCOLES, A_LAS_SIETE, 12).personas()).isEqualTo(12);
            assertThatThrownBy(() -> SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(), MIERCOLES,
                    A_LAS_SIETE, 2, "x".repeat(501), OPERADOR, AHORA)).hasMessageStartingWith("nota:");
        }

        private SolicitudDisponibilidad crear(OfertaSolicitable oferta, LocalDate fecha, CamposDelTipo campos,
                                              int personas) {
            return SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, oferta, fecha, campos, personas, null,
                    OPERADOR, AHORA);
        }
    }

    @Nested
    @DisplayName("el operador atiende (RF-37 a RF-40)")
    class Atender {

        @Test
        @DisplayName("Atendida con sus condiciones y la ventana de 3 h desde la atención (RF-55)")
        void quedaAtendida() {
            SolicitudDisponibilidad solicitud = pendiente();

            solicitud.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));

            assertThat(solicitud.estado()).isEqualTo(ATENDIDA);
            assertThat(solicitud.condiciones().precioBase()).isEqualTo(300_000);
            assertThat(solicitud.venceEn()).isEqualTo(FIN_VENTANA);
            assertThat(solicitud.historial()).last().satisfies(transicion -> {
                assertThat(transicion.de()).isEqualTo(PENDIENTE);
                assertThat(transicion.actor()).isEqualTo(ActorTransicion.OPERADOR);
                assertThat(transicion.actorId()).isEqualTo(OPERADOR);
            });
        }

        @Test
        @DisplayName("el aparte queda registrado con la vigencia de la ventana: atendida a las 15:00, hasta las 18:00; "
                + "a las 20:30, hasta las 08:00 del día siguiente (RF-38)")
        void aparte() {
            SolicitudDisponibilidad tarde = pendiente();
            SolicitudDisponibilidad noche = pendiente();
            assertThat(tarde.aparteHasta()).isNull();

            tarde.atender(OPERADOR, condiciones(), Instant.parse("2026-10-24T20:00:00Z"));
            noche.atender(OPERADOR, condiciones(), Instant.parse("2026-10-25T01:30:00Z"));

            assertThat(tarde.aparteHasta()).isEqualTo(Instant.parse("2026-10-24T23:00:00Z"));
            assertThat(noche.aparteHasta()).isEqualTo(Instant.parse("2026-10-25T13:00:00Z"));
            tarde.vencer(Instant.parse("2026-10-24T23:00:00Z"));
            assertThat(tarde.estado()).isEqualTo(VENCIDA);
            assertThat(tarde.venceEn()).isNull();
            assertThat(tarde.aparteHasta()).isEqualTo(Instant.parse("2026-10-24T23:00:00Z"));
        }

        @Test
        @DisplayName("de noche, la ventana llega hasta las 08:00 del día siguiente")
        void deNoche() {
            SolicitudDisponibilidad solicitud = pendiente();
            Instant lasVeinte = Instant.parse("2026-10-25T01:00:00Z");

            solicitud.atender(OPERADOR, condiciones(), lasVeinte);

            assertThat(solicitud.venceEn()).isEqualTo(Instant.parse("2026-10-25T13:00:00Z"));
        }

        @Test
        @DisplayName("si la ventana con su gracia terminaría después del inicio del servicio, solo queda No disponible")
        void sinTiempo() {
            // Enviada el lunes 26 a las 22:00 para el miércoles 28 a las 07:00 y atendida el martes a las 21:00, dentro
            // de las 24 h: la ventana vence el miércoles a las 08:00, una hora después de la salida.
            SolicitudDisponibilidad solicitud = pendiente(LUNES_22);

            assertThatThrownBy(() -> solicitud.atender(OPERADOR, condiciones(), MARTES_21))
                    .isInstanceOf(SinTiempoParaConfirmarException.class)
                    .extracting(e -> ((ExcepcionDominio) e).codigo()).isEqualTo("SIN_TIEMPO_PARA_CONFIRMAR");
            assertThat(solicitud.estado()).isEqualTo(PENDIENTE);

            solicitud.marcarNoDisponible(OPERADOR, null, MARTES_21);
            assertThat(solicitud.estado()).isEqualTo(NO_DISPONIBLE);
        }

        @Test
        @DisplayName("la gracia cuenta: si con ella termina justo al inicio del servicio se puede; después, no")
        void bordeDelInicio() {
            // Atendida el martes a las 21:00: la ventana vence a las 08:00 y la gracia, a las 08:30.
            OfertaSolicitable dosSalidas = new OfertaSolicitable(caminata().id(), caminata().datos(),
                    List.of(LocalTime.of(8, 0), LocalTime.of(8, 30)), 12);
            SolicitudDisponibilidad alasOchoYMedia = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA,
                    dosSalidas, MIERCOLES, CamposDelTipo.actividad(LocalTime.of(8, 30)), 2, null, OPERADOR, LUNES_22);
            SolicitudDisponibilidad alasOcho = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA,
                    dosSalidas, MIERCOLES, CamposDelTipo.actividad(LocalTime.of(8, 0)), 2, null, OPERADOR, LUNES_22);

            alasOchoYMedia.atender(OPERADOR, condiciones(), MARTES_21);

            assertThat(alasOchoYMedia.estado()).isEqualTo(ATENDIDA);
            assertThatThrownBy(() -> alasOcho.atender(OPERADOR, condiciones(), MARTES_21))
                    .isInstanceOf(SinTiempoParaConfirmarException.class);
        }

        @Test
        @DisplayName("No disponible con un motivo opcional; ya no tiene plazo")
        void noDisponible() {
            SolicitudDisponibilidad solicitud = pendiente();

            solicitud.marcarNoDisponible(OPERADOR, " Sin cupo ese día ", AHORA.plusSeconds(60));

            assertThat(solicitud.estado()).isEqualTo(NO_DISPONIBLE);
            assertThat(solicitud.venceEn()).isNull();
            assertThat(solicitud.historial()).last().extracting(TransicionSolicitud::motivo)
                    .isEqualTo("Sin cupo ese día");
        }

        @Test
        @DisplayName("pasadas las 24 h no se puede atender aunque el vencimiento todavía no haya corrido")
        void plazoVencido() {
            SolicitudDisponibilidad solicitud = pendiente();

            assertThatThrownBy(() -> solicitud.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(24))))
                    .isInstanceOf(PlazoVencidoException.class);
        }

        @Test
        @DisplayName("una solicitud ya atendida no se atiende otra vez ni se marca no disponible: sus datos no cambian")
        void unaSolaVez() {
            SolicitudDisponibilidad solicitud = atendida();

            assertThatThrownBy(() -> solicitud.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(2))))
                    .isInstanceOf(TransicionInvalidaException.class)
                    .extracting(e -> ((ExcepcionDominio) e).codigo()).isEqualTo("TRANSICION_INVALIDA");
            assertThatThrownBy(() -> solicitud.marcarNoDisponible(OPERADOR, null, AHORA.plus(Duration.ofHours(2))))
                    .isInstanceOf(TransicionInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("el turista responde (RF-42, RF-43)")
    class Responder {

        @Test
        @DisplayName("confirmar: Confirmada, con la misma ventana")
        void confirmar() {
            SolicitudDisponibilidad solicitud = atendida();

            solicitud.responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA.plus(Duration.ofHours(2)));

            assertThat(solicitud.estado()).isEqualTo(CONFIRMADA);
            assertThat(solicitud.venceEn()).isEqualTo(FIN_VENTANA);
        }

        @Test
        @DisplayName("declinar: Declinada, sin plazo")
        void declinar() {
            SolicitudDisponibilidad solicitud = atendida();

            solicitud.responder(TURISTA, DecisionTurista.DECLINAR, AHORA.plus(Duration.ofHours(2)));

            assertThat(solicitud.estado()).isEqualTo(DECLINADA);
            assertThat(solicitud.venceEn()).isNull();
        }

        @Test
        @DisplayName("una pendiente no se confirma; una confirmada no se declina; fuera de la ventana, plazo vencido")
        void invalidas() {
            assertThatThrownBy(() -> pendiente().responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA))
                    .isInstanceOf(TransicionInvalidaException.class).hasMessageContaining("PENDIENTE");
            assertThatThrownBy(() -> confirmada().responder(TURISTA, DecisionTurista.DECLINAR, AHORA))
                    .isInstanceOf(TransicionInvalidaException.class);
            assertThatThrownBy(() -> atendida().responder(TURISTA, DecisionTurista.CONFIRMAR, FIN_VENTANA))
                    .isInstanceOf(PlazoVencidoException.class);
        }
    }

    @Nested
    @DisplayName("vencimientos (RF-41, RF-55)")
    class Vencer {

        @Test
        @DisplayName("una pendiente pasa a Sin respuesta a las 24 h exactas, no antes")
        void sinRespuesta() {
            SolicitudDisponibilidad solicitud = pendiente();

            assertThat(solicitud.vencer(AHORA.plus(Duration.ofHours(24)).minusMillis(1))).isFalse();
            assertThat(solicitud.vencer(AHORA.plus(Duration.ofHours(24)))).isTrue();

            assertThat(solicitud.estado()).isEqualTo(SIN_RESPUESTA);
            assertThat(solicitud.historial()).last().satisfies(transicion -> {
                assertThat(transicion.actor()).isEqualTo(ActorTransicion.SISTEMA);
                assertThat(transicion.motivo()).contains("24 horas");
            });
        }

        @Test
        @DisplayName("atendida o confirmada vencen al terminar la ventana")
        void vencida() {
            SolicitudDisponibilidad atendida = atendida();
            SolicitudDisponibilidad confirmada = confirmada();

            assertThat(atendida.vencer(FIN_VENTANA.minusMillis(1))).isFalse();
            assertThat(atendida.vencer(FIN_VENTANA)).isTrue();
            assertThat(confirmada.vencer(FIN_VENTANA)).isTrue();

            assertThat(atendida.estado()).isEqualTo(VENCIDA);
            assertThat(confirmada.estado()).isEqualTo(VENCIDA);
            assertThat(confirmada.venceEn()).isNull();
        }

        @Test
        @DisplayName("con un intento de pago en curso espera hasta 30 min después de la ventana, no más")
        void graciaDelPago() {
            SolicitudDisponibilidad solicitud = confirmada();
            solicitud.marcarIntentoEnCurso(FIN_VENTANA.minusSeconds(60));

            assertThat(solicitud.limiteVencimiento()).isEqualTo(FIN_VENTANA.plus(Duration.ofMinutes(30)));
            assertThat(solicitud.vencer(FIN_VENTANA)).isFalse();
            assertThat(solicitud.vencer(FIN_VENTANA.plus(Duration.ofMinutes(29)))).isFalse();
            assertThat(solicitud.vencer(FIN_VENTANA.plus(Duration.ofMinutes(30)))).isTrue();

            assertThat(solicitud.estado()).isEqualTo(VENCIDA);
            assertThat(solicitud.intentoPagoEnCurso()).isFalse();
            assertThat(solicitud.historial()).last().extracting(TransicionSolicitud::motivo).asString()
                    .contains("gracia");
        }

        @Test
        @DisplayName("una cerrada no vence otra vez")
        void cerrada() {
            SolicitudDisponibilidad solicitud = pendiente();
            solicitud.vencer(AHORA.plus(Duration.ofHours(24)));

            assertThat(solicitud.vencer(AHORA.plus(Duration.ofDays(30)))).isFalse();
            assertThat(solicitud.limiteVencimiento()).isNull();
            assertThat(solicitud.historial()).hasSize(2);
        }

        @Test
        @DisplayName("próxima a vencer: pendiente con 6 h o menos (RF-36); una atendida nunca")
        void proximaAVencer() {
            SolicitudDisponibilidad solicitud = pendiente();

            assertThat(solicitud.proximaAVencer(AHORA.plus(Duration.ofHours(17)).plusSeconds(59))).isFalse();
            assertThat(solicitud.proximaAVencer(AHORA.plus(Duration.ofHours(18)))).isTrue();
            assertThat(atendida().proximaAVencer(FIN_VENTANA.minusSeconds(60))).isFalse();
        }
    }

    @Nested
    @DisplayName("pagos (RF-50, RF-55, RF-58)")
    class Pagos {

        @Test
        @DisplayName("pago aprobado: Pagada y sin plazo; repetido, sin cambios")
        void aprobado() {
            SolicitudDisponibilidad solicitud = confirmada();
            solicitud.marcarIntentoEnCurso(AHORA.plus(Duration.ofHours(3)));

            assertThat(solicitud.registrarPagoAprobado(AHORA.plus(Duration.ofHours(3))))
                    .isEqualTo(ResultadoEventoPago.APLICADO);
            assertThat(solicitud.estado()).isEqualTo(PAGADA);
            assertThat(solicitud.intentoPagoEnCurso()).isFalse();
            assertThat(solicitud.venceEn()).isNull();
            assertThat(solicitud.registrarPagoAprobado(AHORA.plus(Duration.ofHours(4))))
                    .isEqualTo(ResultadoEventoPago.SIN_CAMBIOS);
        }

        @Test
        @DisplayName("intento abandonado: deja de aplazar el vencimiento y no cuenta como rechazo (DN-20)")
        void intentoLiberado() {
            SolicitudDisponibilidad solicitud = confirmada();
            solicitud.marcarIntentoEnCurso(FIN_VENTANA.minusSeconds(60));
            assertThat(solicitud.vencer(FIN_VENTANA.plus(Duration.ofMinutes(10)))).isFalse();

            assertThat(solicitud.liberarIntentoEnCurso(FIN_VENTANA.plus(Duration.ofMinutes(20))))
                    .isEqualTo(ResultadoEventoPago.APLICADO);

            assertThat(solicitud.intentoPagoEnCurso()).isFalse();
            assertThat(solicitud.intentosPagoFallidos()).isZero();
            assertThat(solicitud.limiteVencimiento()).isEqualTo(FIN_VENTANA);
            assertThat(solicitud.liberarIntentoEnCurso(FIN_VENTANA.plus(Duration.ofMinutes(21))))
                    .isEqualTo(ResultadoEventoPago.SIN_CAMBIOS);
            assertThat(solicitud.vencer(FIN_VENTANA.plus(Duration.ofMinutes(21)))).isTrue();
            assertThat(solicitud.estado()).isEqualTo(VENCIDA);
            assertThat(solicitud.liberarIntentoEnCurso(FIN_VENTANA.plus(Duration.ofMinutes(22))))
                    .isEqualTo(ResultadoEventoPago.SIN_CAMBIOS);
        }

        @Test
        @DisplayName("aprobado durante la gracia: Pagada; aprobado sobre una vencida: pago tardío, no la revive")
        void tardio() {
            SolicitudDisponibilidad enGracia = confirmada();
            enGracia.marcarIntentoEnCurso(FIN_VENTANA.minusSeconds(60));
            assertThat(enGracia.registrarPagoAprobado(FIN_VENTANA.plus(Duration.ofMinutes(10))))
                    .isEqualTo(ResultadoEventoPago.APLICADO);
            assertThat(enGracia.estado()).isEqualTo(PAGADA);

            SolicitudDisponibilidad vencida = confirmada();
            vencida.vencer(FIN_VENTANA);
            assertThat(vencida.registrarPagoAprobado(FIN_VENTANA.plusSeconds(60)))
                    .isEqualTo(ResultadoEventoPago.PAGO_TARDIO);
            assertThat(vencida.estado()).isEqualTo(VENCIDA);
        }

        @Test
        @DisplayName("aprobado después de la ventana sin que el vencimiento hubiera corrido: la vence y es tardío")
        void tardioSinVencimientoAplicado() {
            SolicitudDisponibilidad solicitud = confirmada();

            assertThat(solicitud.registrarPagoAprobado(FIN_VENTANA.plusSeconds(10)))
                    .isEqualTo(ResultadoEventoPago.PAGO_TARDIO);
            assertThat(solicitud.estado()).isEqualTo(VENCIDA);
        }

        @Test
        @DisplayName("rechazos: Pago fallido dos veces y al tercero Vencida (tres intentos en total)")
        void tresIntentos() {
            SolicitudDisponibilidad solicitud = confirmada();
            Instant hora = AHORA.plus(Duration.ofHours(2));

            for (int intento = 1; intento <= 2; intento++) {
                solicitud.marcarIntentoEnCurso(hora);
                assertThat(solicitud.registrarPagoRechazado(hora)).isEqualTo(ResultadoEventoPago.APLICADO);
                assertThat(solicitud.estado()).isEqualTo(PAGO_FALLIDO);
            }
            assertThat(solicitud.historial()).last().extracting(TransicionSolicitud::de).isEqualTo(PAGO_FALLIDO);
            solicitud.marcarIntentoEnCurso(hora);
            solicitud.registrarPagoRechazado(hora);

            assertThat(solicitud.estado()).isEqualTo(VENCIDA);
            assertThat(solicitud.intentosPagoFallidos()).isEqualTo(3);
            assertThat(solicitud.historial()).last().extracting(TransicionSolicitud::motivo).asString()
                    .contains("tercer");
        }

        @Test
        @DisplayName("rechazado durante la gracia: ya no hay tiempo para reintentar, Vencida")
        void rechazadoEnGracia() {
            SolicitudDisponibilidad solicitud = confirmada();
            solicitud.marcarIntentoEnCurso(FIN_VENTANA.minusSeconds(60));

            solicitud.registrarPagoRechazado(FIN_VENTANA.plus(Duration.ofMinutes(5)));

            assertThat(solicitud.estado()).isEqualTo(VENCIDA);
        }

        @Test
        @DisplayName("el intento en curso solo dentro de la ventana y en Confirmada o Pago fallido")
        void intentoEnCurso() {
            assertThatThrownBy(() -> atendida().marcarIntentoEnCurso(AHORA.plus(Duration.ofHours(2))))
                    .isInstanceOf(TransicionInvalidaException.class);
            assertThatThrownBy(() -> confirmada().marcarIntentoEnCurso(FIN_VENTANA))
                    .isInstanceOf(PlazoVencidoException.class);
            SolicitudDisponibilidad solicitud = confirmada();
            Instant enLaVentana = AHORA.plus(Duration.ofHours(2));
            assertThat(solicitud.marcarIntentoEnCurso(enLaVentana)).isEqualTo(ResultadoEventoPago.APLICADO);
            assertThat(solicitud.marcarIntentoEnCurso(enLaVentana)).isEqualTo(ResultadoEventoPago.SIN_CAMBIOS);
        }

        @Test
        @DisplayName("eventos sobre una pendiente o atendida son inválidos; un rechazo sobre una cerrada no cambia")
        void fueraDeLugar() {
            assertThatThrownBy(() -> pendiente().registrarPagoAprobado(AHORA))
                    .isInstanceOf(TransicionInvalidaException.class);
            assertThatThrownBy(() -> atendida().registrarPagoRechazado(AHORA))
                    .isInstanceOf(TransicionInvalidaException.class);
            SolicitudDisponibilidad declinada = atendida();
            declinada.responder(TURISTA, DecisionTurista.DECLINAR, AHORA.plus(Duration.ofHours(2)));
            assertThat(declinada.registrarPagoRechazado(AHORA.plus(Duration.ofHours(2))))
                    .isEqualTo(ResultadoEventoPago.SIN_CAMBIOS);
        }
    }

    @Test
    @DisplayName("tiene cambios solo si algo cambió: un evento sin efecto no obliga a guardar")
    void cambios() {
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.reconstituir(UUID.randomUUID(), TURISTA,
                "b1", caminata().datos(), MIERCOLES, A_LAS_SIETE, 2, null, OPERADOR, AHORA,
                EstadoSolicitud.PAGADA, condiciones(), null, null, false, 0, AHORA, 3, java.util.List.of());

        assertThat(solicitud.registrarPagoAprobado(AHORA)).isEqualTo(ResultadoEventoPago.SIN_CAMBIOS);
        assertThat(solicitud.tieneCambios()).isFalse();
        SolicitudDisponibilidad confirmada = confirmada();
        assertThat(confirmada.tieneCambios()).isTrue();
    }

    @Test
    @DisplayName("el inicio del servicio es la fecha y la hora de su tipo en Colombia; el alojamiento, a las 00:00")
    void inicioServicio() {
        assertThat(pendiente().inicioServicio()).isEqualTo(Instant.parse("2026-10-28T12:00:00Z"));
        SolicitudDisponibilidad cena = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(),
                MIERCOLES, CamposDelTipo.restaurante(LocalTime.of(19, 30)), 4, null, OPERADOR, AHORA);
        assertThat(cena.inicioServicio()).isEqualTo(Instant.parse("2026-10-29T00:30:00Z"));
        SolicitudDisponibilidad cabana = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(),
                MIERCOLES, dosNoches(MIERCOLES), 2, null, OPERADOR, AHORA);
        assertThat(cabana.inicioServicio()).isEqualTo(Instant.parse("2026-10-28T05:00:00Z"));
    }

    @Test
    @DisplayName("RF-39 con la hora de cada tipo: una reserva a las 07:30 no alcanza; una a las 12:00, sí")
    void sinTiempoPorTipo() {
        // Enviadas el lunes 26 a las 22:00 y atendidas el martes a las 21:00: la ventana vence el miércoles a las
        // 08:00 y la gracia, a las 08:30.
        SolicitudDisponibilidad desayuno = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(),
                MIERCOLES, CamposDelTipo.restaurante(LocalTime.of(7, 30)), 4, null, OPERADOR, LUNES_22);
        SolicitudDisponibilidad almuerzo = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(),
                MIERCOLES, CamposDelTipo.restaurante(LocalTime.NOON), 4, null, OPERADOR, LUNES_22);
        SolicitudDisponibilidad cabana = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(),
                MIERCOLES, dosNoches(MIERCOLES), 2, null, OPERADOR, LUNES_22);

        assertThatThrownBy(() -> desayuno.atender(OPERADOR, condiciones(), MARTES_21))
                .isInstanceOf(SinTiempoParaConfirmarException.class);
        assertThatThrownBy(() -> cabana.atender(OPERADOR, condiciones(), MARTES_21))
                .isInstanceOf(SinTiempoParaConfirmarException.class);
        almuerzo.atender(OPERADOR, condiciones(), MARTES_21);
        assertThat(almuerzo.estado()).isEqualTo(ATENDIDA);
    }
}
