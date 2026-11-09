package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

import java.util.UUID;

/**
 * Lo que otro módulo hace cuando un prestador registra o cambia su cuenta de cobro (DN-44). La implementa pagos, que
 * reactiva las dispersiones fallidas del prestador (RF-54). Prestadores no puede depender de pagos, que ya depende de
 * él: por eso declara aquí lo que avisa, como {@code cuentas.api.FuenteTipoPrestador} (DN-37).
 */
public interface ObservadorCuentaCobro {

    /** Se llama dentro de la transacción del cambio: si falla, el cambio se deshace. */
    void cuentaCambiada(UUID prestadorId);
}
