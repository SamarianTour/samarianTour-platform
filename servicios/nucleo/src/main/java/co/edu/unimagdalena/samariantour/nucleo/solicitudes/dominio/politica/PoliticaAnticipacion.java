package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import java.time.Instant;
import java.time.LocalDate;

/**
 * La fecha de inicio del servicio debe ser al menos 2 días calendario después del envío, en la hora de Colombia
 * (RF-30). Son días de calendario, no 48 h: enviada el lunes a las 23:00, la primera fecha posible es el miércoles.
 */
public final class PoliticaAnticipacion {

    public static final int DIAS_MINIMOS = 2;

    private PoliticaAnticipacion() {
    }

    /** La primera fecha de inicio que admite una solicitud enviada en ese momento. */
    public static LocalDate primeraFechaPermitida(Instant enviadaEn) {
        return enviadaEn.atZone(PoliticaVentanas.ZONA).toLocalDate().plusDays(DIAS_MINIMOS);
    }

    public static boolean permite(LocalDate fechaInicio, Instant enviadaEn) {
        return !fechaInicio.isBefore(primeraFechaPermitida(enviadaEn));
    }
}
