package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Los días de un indicador, en hora de Colombia, con los dos extremos incluidos (RF-85 a RF-89). Por defecto, los
 * últimos 30 días; como máximo 366 [SUPUESTO] (DN-41).
 */
public record Periodo(LocalDate desde, LocalDate hasta) {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    public static final int DIAS_POR_DEFECTO = 30;
    public static final int DIAS_MAXIMOS = 366;

    public Periodo {
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hasta, "hasta");
        if (desde.isAfter(hasta)) {
            throw new DatoInvalidoException("desde", "no puede ser posterior a hasta");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > DIAS_MAXIMOS) {
            throw new DatoInvalidoException("hasta", "el periodo es de %d días como máximo".formatted(DIAS_MAXIMOS));
        }
    }

    /** Sin {@code hasta}, termina hoy; sin {@code desde}, empieza 29 días antes del final. */
    public static Periodo de(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate fin = hasta == null ? hoy : hasta;
        return new Periodo(desde == null ? fin.minusDays(DIAS_POR_DEFECTO - 1) : desde, fin);
    }

    /** El primer instante del periodo. */
    public Instant inicio() {
        return desde.atStartOfDay(ZONA).toInstant();
    }

    /** El primer instante después del periodo: la medianoche que sigue a {@code hasta}. */
    public Instant fin() {
        return hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
    }
}
