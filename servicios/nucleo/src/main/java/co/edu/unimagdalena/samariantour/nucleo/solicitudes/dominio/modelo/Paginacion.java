package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;

/** Qué página se pide, desde 0, y de qué tamaño: como en el catálogo, hasta 100 por página. */
public record Paginacion(int pagina, int tamano) {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    public Paginacion {
        if (pagina < 0) {
            throw new DatoInvalidoException("page", "empieza en 0");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new DatoInvalidoException("size", "debe estar entre 1 y " + TAMANO_MAXIMO);
        }
    }
}
