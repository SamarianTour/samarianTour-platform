package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

import java.util.Objects;

/** El contacto de un turista con el WhatsApp verificado (RF-03, RF-44, RF-57). */
public record ContactoTurista(String nombre, String whatsapp) {

    public ContactoTurista {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(whatsapp, "whatsapp");
    }

    @Override
    public String toString() {
        return "ContactoTurista[whatsapp=******]";
    }
}
