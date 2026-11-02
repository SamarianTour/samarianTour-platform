package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;

import java.util.UUID;

/** El administrador da de baja una cuenta interna o la vuelve a activar (RF-81). */
public interface DesactivarCuentaInternaUseCase {

    /**
     * Ya no inicia sesión y se cierran sus sesiones.
     *
     * @throws CuentaNoEncontradaException si no existe o no es interna
     */
    Usuario desactivar(UUID usuarioId);

    /** @throws CuentaNoEncontradaException si no existe o no es interna */
    Usuario reactivar(UUID usuarioId);
}
