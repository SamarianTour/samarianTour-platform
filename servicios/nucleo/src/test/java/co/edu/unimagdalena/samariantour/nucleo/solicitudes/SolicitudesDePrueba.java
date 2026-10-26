package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CondicionesOfrecidas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DatosOfertaCopiados;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OfertaSolicitable;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoOferta;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Datos de prueba de las solicitudes, con los identificadores de la demo del catálogo. AHORA es el sábado 24 de octubre
 * de 2026 a las 10:00 en Colombia; la caminata es el miércoles 28 a las 07:00.
 */
public final class SolicitudesDePrueba {

    public static final UUID TURISTA = UUID.fromString("90000000-0000-0000-0000-000000000001");
    public static final UUID OTRO_TURISTA = UUID.fromString("90000000-0000-0000-0000-000000000002");
    public static final UUID OPERADOR = UUID.fromString("60000000-0000-0000-0000-000000000001");
    public static final UUID OTRO_OPERADOR = UUID.fromString("60000000-0000-0000-0000-000000000002");
    public static final UUID AGENCIA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID HOTEL = UUID.fromString("30000000-0000-0000-0000-000000000003");
    public static final UUID RESTAURANTE = UUID.fromString("40000000-0000-0000-0000-000000000004");
    public static final String CABO_SAN_JUAN = "b00000000000000000000001";
    public static final String CABANA = "b0000000000000000000000f";
    public static final String COCINA_CARIBE = "b00000000000000000000013";
    public static final String TAYRONA = "a00000000000000000000005";

    /** Sábado 24 de octubre de 2026, 10:00 en Colombia. */
    public static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    public static final LocalDate MIERCOLES = LocalDate.parse("2026-10-28");
    public static final LocalTime SIETE = LocalTime.of(7, 0);

    private SolicitudesDePrueba() {
    }

    public static OfertaSolicitable caminata() {
        return new OfertaSolicitable(CABO_SAN_JUAN, new DatosOfertaCopiados(TipoOferta.ACTIVIDAD,
                "Caminata a Cabo San Juan del Guía", AGENCIA, TAYRONA), List.of(SIETE, LocalTime.of(8, 0)), 12);
    }

    public static OfertaSolicitable cabana() {
        return new OfertaSolicitable(CABANA, new DatosOfertaCopiados(TipoOferta.ALOJAMIENTO,
                "Cabaña frente al mar en Buritaca", HOTEL, "a00000000000000000000006"), List.of(), 4);
    }

    public static OfertaSolicitable cocinaCaribe() {
        return new OfertaSolicitable(COCINA_CARIBE, new DatosOfertaCopiados(TipoOferta.RESTAURANTE,
                "Cocina del Caribe en el Centro Histórico", RESTAURANTE, "a00000000000000000000001"), List.of(), 12);
    }

    /** Dos noches en una cabaña desde la fecha de inicio. */
    public static CamposDelTipo dosNoches(LocalDate inicio) {
        return CamposDelTipo.alojamiento(inicio.plusDays(2), 1);
    }

    public static CondicionesOfrecidas condiciones() {
        return new CondicionesOfrecidas(300_000, "Sin reembolso a menos de 24 h", "Incluye el ingreso al parque");
    }

    /** Pendiente, enviada AHORA, sin notificaciones por extraer. */
    public static SolicitudDisponibilidad pendiente() {
        return pendiente(AHORA);
    }

    public static SolicitudDisponibilidad pendiente(Instant enviadaEn) {
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                MIERCOLES, CamposDelTipo.actividad(SIETE), 2, "Vamos con una niña de 8 años", OPERADOR, enviadaEn);
        solicitud.extraerNotificaciones();
        return solicitud;
    }

    /** Atendida una hora después de enviada: la ventana vence a las 14:00 en Colombia. */
    public static SolicitudDisponibilidad atendida() {
        SolicitudDisponibilidad solicitud = pendiente();
        solicitud.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));
        solicitud.extraerNotificaciones();
        return solicitud;
    }

    public static SolicitudDisponibilidad confirmada() {
        SolicitudDisponibilidad solicitud = atendida();
        solicitud.responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA.plus(Duration.ofHours(2)));
        solicitud.extraerNotificaciones();
        return solicitud;
    }
}
