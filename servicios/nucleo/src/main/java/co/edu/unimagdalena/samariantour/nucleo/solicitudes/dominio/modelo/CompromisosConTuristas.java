package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/**
 * Lo que un prestador todavía le debe a los turistas, que le impide suprimir sus datos (RF-13, DN-45).
 *
 * @param pagadasPorPrestar las solicitudes pagadas cuyo servicio no ha terminado: la fecha de salida del alojamiento,
 *                          o la de inicio de lo demás, es hoy o después
 * @param pagosEnCurso      las solicitudes abiertas con un intento de pago en curso, que todavía puede aprobarse
 */
public record CompromisosConTuristas(long pagadasPorPrestar, long pagosEnCurso) {
}
