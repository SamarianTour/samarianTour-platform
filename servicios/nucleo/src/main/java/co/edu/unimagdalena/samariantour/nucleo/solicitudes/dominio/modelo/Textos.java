package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;

/** Normalización de los textos libres de la solicitud. */
final class Textos {

    private Textos() {
    }

    /** Sin espacios en los extremos; vacío es null. */
    static String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    static void verificarLongitud(String campo, String texto, int maximo) {
        if (texto != null && texto.length() > maximo) {
            throw new DatoInvalidoException(campo, "tiene como máximo %d caracteres".formatted(maximo));
        }
    }
}
