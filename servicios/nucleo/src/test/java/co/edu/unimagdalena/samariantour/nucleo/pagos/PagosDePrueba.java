package co.edu.unimagdalena.samariantour.nucleo.pagos;

import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.FormatoWompi;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OfertaSolicitable;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;

/**
 * Datos de prueba de pagos. Las solicitudes confirmadas tienen su ventana hasta AHORA + 4 h (las 14:00 en Colombia) y
 * cuestan 300.000 pesos. Los eventos se firman con el secreto de la pasarela simulada, como en la demo.
 */
public final class PagosDePrueba {

    /** El secreto de eventos por defecto de la pasarela simulada (no es de Wompi). */
    public static final String SECRETO_SIMULADA = "secreto-de-eventos-solo-para-la-demo";
    public static final Instant FIN_VENTANA = AHORA.plus(Duration.ofHours(4));
    public static final long PRECIO_CENTAVOS = 30_000_000L;
    public static final UUID ADMINISTRADOR = UUID.fromString("70000000-0000-0000-0000-000000000001");
    public static final UUID RESTAURANTE = UUID.fromString("40000000-0000-0000-0000-000000000004");

    private PagosDePrueba() {
    }

    /** Un reloj que la prueba mueve. */
    public static Clock reloj(AtomicReference<Instant> ahora) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("America/Bogota");
            }

            @Override
            public Clock withZone(ZoneId zona) {
                return this;
            }

            @Override
            public Instant instant() {
                return ahora.get();
            }
        };
    }

    /** Confirmada por la turista de la demo para la caminata en esa fecha: atendida a AHORA + 1 h. */
    public static SolicitudDisponibilidad confirmada(LocalDate fecha) {
        return confirmada(SolicitudesDePrueba.caminata(), fecha, CamposDelTipo.actividad(SolicitudesDePrueba.SIETE));
    }

    /** Confirmada para el restaurante de la demo, cuya cuenta falla en la dispersión simulada. */
    public static SolicitudDisponibilidad confirmadaRestaurante(LocalDate fecha) {
        return confirmada(SolicitudesDePrueba.cocinaCaribe(), fecha,
                CamposDelTipo.restaurante(java.time.LocalTime.of(13, 0)));
    }

    /** Atendida, sin confirmar todavía: no se puede pagar. */
    public static SolicitudDisponibilidad atendida(LocalDate fecha) {
        return atendida(SolicitudesDePrueba.caminata(), fecha, CamposDelTipo.actividad(SolicitudesDePrueba.SIETE));
    }

    private static SolicitudDisponibilidad confirmada(OfertaSolicitable oferta, LocalDate fecha, CamposDelTipo campos) {
        SolicitudDisponibilidad solicitud = atendida(oferta, fecha, campos);
        solicitud.responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA.plus(Duration.ofHours(2)));
        return solicitud;
    }

    private static SolicitudDisponibilidad atendida(OfertaSolicitable oferta, LocalDate fecha, CamposDelTipo campos) {
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, oferta, fecha,
                campos, 2, null, OPERADOR, AHORA);
        solicitud.atender(OPERADOR, SolicitudesDePrueba.condiciones(), AHORA.plus(Duration.ofHours(1)));
        return solicitud;
    }

    /** Un evento {@code transaction.updated} firmado como lo envía la pasarela simulada. */
    public static String evento(String transaccionId, String referencia, long montoCentavos, String estadoWompi,
                                ObjectMapper json) {
        return FormatoWompi.eventoFirmado(transaccionId, referencia, montoCentavos, estadoWompi, "test", 1_761_577_200L,
                "2026-10-27T15:00:00Z", SECRETO_SIMULADA, json);
    }
}
