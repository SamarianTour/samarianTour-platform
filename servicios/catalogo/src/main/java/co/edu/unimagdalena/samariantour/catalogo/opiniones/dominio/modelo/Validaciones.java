package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;

/**
 * Validaciones de campo del dominio de opiniones, con los mismos mensajes que las del catálogo.
 */
public final class Validaciones {

    private Validaciones() {
    }

    /** Texto obligatorio; se devuelve sin espacios al inicio ni al final. */
    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException(campo, "es obligatorio");
        }
        return valor.strip();
    }

    /** Texto opcional; un texto en blanco se trata como ausente. */
    public static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    public static <T> T presente(T valor, String campo) {
        if (valor == null) {
            throw new DatoInvalidoException(campo, "es obligatorio");
        }
        return valor;
    }
}
