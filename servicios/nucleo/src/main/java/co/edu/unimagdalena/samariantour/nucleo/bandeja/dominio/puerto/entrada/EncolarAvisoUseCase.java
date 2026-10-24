package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;

/**
 * Guarda un aviso para el catálogo en la bandeja de salida (DN-06). Debe llamarse dentro de la transacción del
 * cambio que lo origina: si esa transacción se deshace, el aviso tampoco queda.
 */
public interface EncolarAvisoUseCase {

    /**
     * @param cuerpo el aviso en JSON, como lo espera el catálogo
     * @return false si ya había un aviso con ese identificador; no se duplica
     * @throws RuntimeException si no hay una transacción abierta, para no dejar un aviso suelto
     */
    boolean encolar(String idAviso, Destino destino, String cuerpo);
}
