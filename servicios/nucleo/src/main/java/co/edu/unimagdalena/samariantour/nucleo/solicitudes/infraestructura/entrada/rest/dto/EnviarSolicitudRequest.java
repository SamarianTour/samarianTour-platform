package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que envía el turista (RF-30). El turista es el de la identidad; sus datos de contacto ya están registrados y no se
 * vuelven a pedir (RF-03). Cada tipo de oferta lleva sus campos y rechaza los de los demás.
 *
 * @param horaSalida     "HH:mm", una de las horas de salida de la oferta; solo actividad y paquete
 * @param horaReserva    "HH:mm"; solo restaurante
 * @param fechaSalida    posterior a la fecha de inicio; solo alojamiento
 * @param numeroUnidades entero positivo; solo alojamiento
 */
public record EnviarSolicitudRequest(String ofertaId, LocalDate fechaInicio, LocalTime horaSalida,
                                     LocalTime horaReserva, LocalDate fechaSalida, Integer numeroUnidades,
                                     Integer personas, String nota) {
}
