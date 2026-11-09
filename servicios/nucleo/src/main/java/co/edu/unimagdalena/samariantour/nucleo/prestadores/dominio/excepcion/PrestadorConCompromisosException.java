package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

import java.util.List;

/**
 * El prestador todavía tiene compromisos con turistas o dinero por recibir, así que no puede suprimir sus datos
 * (DN-45): primero los cumple. {@link #compromisos()} dice cuáles.
 */
public class PrestadorConCompromisosException extends ExcepcionDominio {

    private final List<String> compromisos;

    public PrestadorConCompromisosException(List<String> compromisos) {
        super("PRESTADOR_CON_COMPROMISOS", "Antes de suprimir sus datos tiene que cumplir: "
                + String.join("; ", compromisos));
        this.compromisos = List.copyOf(compromisos);
    }

    public List<String> compromisos() {
        return compromisos;
    }
}
