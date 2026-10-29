package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CategoriaPrestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoPrestador;

/**
 * La categoría del prestador (diagrama de paquetes, RF-72, RF-89): el guía independiente es su propia categoría; los
 * demás, por empleados: micro y pequeño hasta 50, mediano de 51 a 200 y grande más de 200.
 */
public final class ReglaCategoria {

    public static final int MAXIMO_MICRO_PEQUENO = 50;
    public static final int MAXIMO_MEDIANO = 200;

    private ReglaCategoria() {
    }

    public static CategoriaPrestador de(TipoPrestador tipo, Integer empleados) {
        if (tipo == TipoPrestador.GUIA_INDEPENDIENTE) {
            return CategoriaPrestador.GUIA_INDEPENDIENTE;
        }
        if (empleados == null || empleados <= MAXIMO_MICRO_PEQUENO) {
            return CategoriaPrestador.MICRO_PEQUENO;
        }
        return empleados <= MAXIMO_MEDIANO ? CategoriaPrestador.MEDIANO : CategoriaPrestador.GRANDE;
    }
}
