package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;

/**
 * Página pedida por el cliente, con las reglas de DC-14: la primera es la 0, 20 por defecto y como máximo 100.
 * Propia del módulo, como {@link Pagina}.
 */
public record Paginacion(int pagina, int tamano) {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    public Paginacion {
        if (pagina < 0) {
            throw new DatoInvalidoException("page", "no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new DatoInvalidoException("size", "debe estar entre 1 y " + TAMANO_MAXIMO);
        }
    }

    /** Aplica los valores por defecto a los parámetros que el cliente no envió. */
    public static Paginacion de(Integer pagina, Integer tamano) {
        return new Paginacion(pagina == null ? 0 : pagina, tamano == null ? TAMANO_POR_DEFECTO : tamano);
    }

    public long desplazamiento() {
        return (long) pagina * tamano;
    }
}
