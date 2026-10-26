package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.util.Objects;

/**
 * El contacto del turista que registró y verificó en su cuenta (RF-03): el WhatsApp, su único canal (RF-44), y el
 * nombre que recibe el prestador cuando la solicitud queda Pagada (RF-57).
 */
public record ContactoTurista(String nombre, String whatsapp) {

    public ContactoTurista {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(whatsapp, "whatsapp");
    }
}
