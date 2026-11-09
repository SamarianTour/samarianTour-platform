package co.edu.unimagdalena.samariantour.nucleo.bandeja.api;

import java.time.Instant;

/**
 * Un aviso de la bandeja como lo ve el administrador (DN-44).
 *
 * @param destino     OFERTAS u OPINIONES
 * @param tipo        el {@code tipo} del aviso (MODERACION, HABILITACION, ...), o null si el cuerpo no lo trae
 * @param estado      PENDIENTE, ENVIADO o RECHAZADO
 * @param ultimoError lo que respondió el catálogo la última vez que no lo recibió
 */
public record AvisoAdministrado(String idAviso, String destino, String tipo, String estado, int intentos,
                                String ultimoError, Instant creadoEn) {
}
