package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Los plazos del ciclo de vida de una solicitud (RF-36, RF-41, RF-55, DN-10). Están aquí y no en la configuración a
 * propósito: cambiar las 24 h o las 3 h solo toca esta clase (QAS-05). Todos corren desde el cambio de estado, no desde
 * que la notificación llega al turista (ADR-04, decisión ratificada).
 */
public final class PoliticaVentanas {

    /** La hora de Colombia, la del destino: no tiene horario de verano. */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    /** El operador tiene 24 h desde la creación para atender la solicitud (RF-41). */
    public static final Duration PLAZO_OPERADOR = Duration.ofHours(24);

    /** La ventana de confirmación y pago dura 3 h desde que la solicitud pasa a Atendida (RF-55)... */
    public static final Duration VENTANA_CONFIRMACION = Duration.ofHours(3);

    /** ...salvo que pase entre las 19:00 y las 04:59: entonces vence a las 08:00 siguientes (RF-55). */
    public static final LocalTime INICIO_NOCHE = LocalTime.of(19, 0);
    public static final LocalTime FIN_NOCHE = LocalTime.of(5, 0);
    public static final LocalTime VENCE_TRAS_LA_NOCHE = LocalTime.of(8, 0);

    /** Con un intento de pago en curso, el vencimiento espera su resultado hasta 30 min más (RF-55). */
    public static final Duration GRACIA_PAGO = Duration.ofMinutes(30);

    /** En la cola del operador se resalta la solicitud a la que le quedan 6 h o menos (RF-36, valor ratificado). */
    public static final Duration AVISO_PROXIMA_A_VENCER = Duration.ofHours(6);

    private PoliticaVentanas() {
    }

    /** Hasta cuándo puede atenderla el operador: 24 h después de creada. */
    public static Instant plazoOperador(Instant creadaEn) {
        return creadaEn.plus(PLAZO_OPERADOR);
    }

    /**
     * El fin de la ventana de confirmación y pago para una solicitud atendida en ese momento:
     * <ul>
     *   <li>de 05:00 a 18:59, 3 h después;</li>
     *   <li>de 19:00 a 23:59, a las 08:00 del día siguiente;</li>
     *   <li>de 00:00 a 04:59, a las 08:00 del mismo día.</li>
     * </ul>
     */
    public static Instant ventanaConfirmacion(Instant atendidaEn) {
        ZonedDateTime local = atendidaEn.atZone(ZONA);
        LocalTime hora = local.toLocalTime();
        if (!hora.isBefore(INICIO_NOCHE)) {
            return local.toLocalDate().plusDays(1).atTime(VENCE_TRAS_LA_NOCHE).atZone(ZONA).toInstant();
        }
        if (hora.isBefore(FIN_NOCHE)) {
            return local.toLocalDate().atTime(VENCE_TRAS_LA_NOCHE).atZone(ZONA).toInstant();
        }
        return atendidaEn.plus(VENTANA_CONFIRMACION);
    }

    /** El último momento en que puede vencer una ventana con un intento de pago en curso. */
    public static Instant conGracia(Instant venceEn) {
        return venceEn.plus(GRACIA_PAGO);
    }

    /** Si al plazo le quedan 6 h o menos; también si ya pasó y el vencimiento todavía no se aplicó. */
    public static boolean proximaAVencer(Instant venceEn, Instant ahora) {
        return !ahora.plus(AVISO_PROXIMA_A_VENCER).isBefore(venceEn);
    }
}
