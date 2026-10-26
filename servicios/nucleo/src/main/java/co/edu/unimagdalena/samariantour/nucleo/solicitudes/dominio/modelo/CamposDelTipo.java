package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Los datos propios del tipo de oferta que pide la solicitud (RF-30). Cada tipo exige los suyos y rechaza los de los
 * demás:
 * <ul>
 *   <li>actividad y paquete: el horario de salida, uno de los de la oferta;</li>
 *   <li>alojamiento: la fecha de salida, posterior a la de inicio, y el número de unidades;</li>
 *   <li>restaurante: la hora de la reserva.</li>
 * </ul>
 *
 * @param horaSalida     solo actividad y paquete
 * @param horaReserva    solo restaurante
 * @param fechaSalida    solo alojamiento
 * @param numeroUnidades solo alojamiento; entero positivo
 */
public record CamposDelTipo(LocalTime horaSalida, LocalTime horaReserva, LocalDate fechaSalida,
                            Integer numeroUnidades) {

    public static CamposDelTipo actividad(LocalTime horaSalida) {
        return new CamposDelTipo(horaSalida, null, null, null);
    }

    public static CamposDelTipo alojamiento(LocalDate fechaSalida, Integer numeroUnidades) {
        return new CamposDelTipo(null, null, fechaSalida, numeroUnidades);
    }

    public static CamposDelTipo restaurante(LocalTime horaReserva) {
        return new CamposDelTipo(null, horaReserva, null, null);
    }

    /** Los que corresponden al tipo de la oferta, completos y válidos; cualquier otro es un dato inválido. */
    void verificar(OfertaSolicitable oferta, LocalDate fechaInicio) {
        TipoOferta tipo = oferta.datos().tipo();
        switch (tipo) {
            case ACTIVIDAD, PAQUETE -> {
                rechazar(tipo, "horaReserva", horaReserva);
                rechazar(tipo, "fechaSalida", fechaSalida);
                rechazar(tipo, "numeroUnidades", numeroUnidades);
                if (horaSalida == null) {
                    throw new DatoInvalidoException("horaSalida", "es obligatoria en actividades y paquetes: elige "
                            + "una de " + oferta.horariosSalida());
                }
                if (!oferta.horariosSalida().contains(horaSalida)) {
                    throw new DatoInvalidoException("horaSalida", "debe ser uno de los horarios de salida de la "
                            + "oferta: " + oferta.horariosSalida());
                }
            }
            case ALOJAMIENTO -> {
                rechazar(tipo, "horaSalida", horaSalida);
                rechazar(tipo, "horaReserva", horaReserva);
                if (fechaSalida == null) {
                    throw new DatoInvalidoException("fechaSalida", "es obligatoria en alojamientos");
                }
                if (!fechaSalida.isAfter(fechaInicio)) {
                    throw new DatoInvalidoException("fechaSalida", "debe ser posterior a la fecha de inicio, "
                            + fechaInicio);
                }
                if (numeroUnidades == null) {
                    throw new DatoInvalidoException("numeroUnidades", "es obligatorio en alojamientos");
                }
                if (numeroUnidades < 1) {
                    throw new DatoInvalidoException("numeroUnidades", "debe ser un entero positivo");
                }
            }
            case RESTAURANTE -> {
                rechazar(tipo, "horaSalida", horaSalida);
                rechazar(tipo, "fechaSalida", fechaSalida);
                rechazar(tipo, "numeroUnidades", numeroUnidades);
                if (horaReserva == null) {
                    throw new DatoInvalidoException("horaReserva", "es obligatoria en restaurantes");
                }
            }
        }
    }

    private static void rechazar(TipoOferta tipo, String campo, Object valor) {
        if (valor != null) {
            throw new DatoInvalidoException(campo, "no aplica a %s: %s".formatted(nombre(tipo), aplica(campo)));
        }
    }

    private static String nombre(TipoOferta tipo) {
        return switch (tipo) {
            case ACTIVIDAD -> "una actividad";
            case PAQUETE -> "un paquete";
            case ALOJAMIENTO -> "un alojamiento";
            case RESTAURANTE -> "un restaurante";
        };
    }

    private static String aplica(String campo) {
        return switch (campo) {
            case "horaSalida" -> "solo va en actividades y paquetes";
            case "horaReserva" -> "solo va en restaurantes";
            default -> "solo va en alojamientos";
        };
    }

    /**
     * La hora del servicio: el horario de salida o la hora de la reserva. Vacía en alojamiento, que se distingue solo
     * por la fecha de inicio (RF-31).
     */
    public LocalTime hora() {
        return horaSalida != null ? horaSalida : horaReserva;
    }
}
