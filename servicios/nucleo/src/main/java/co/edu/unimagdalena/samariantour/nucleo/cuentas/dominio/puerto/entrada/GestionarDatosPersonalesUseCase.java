package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.ActualizarDatosCommand;

import java.util.UUID;

/** El titular consulta y actualiza sus datos personales (RF-12). */
public interface GestionarDatosPersonalesUseCase {

    /** @throws CuentaNoEncontradaException si la cuenta no existe o está suprimida */
    Usuario consultar(UUID usuarioId);

    /**
     * Cambia el nombre y, para el turista, el WhatsApp. Un WhatsApp distinto queda sin verificar y recibe un código
     * nuevo (RF-03). El correo no se cambia (DN-21).
     *
     * @throws DatoInvalidoException       si un dato no cumple las reglas
     * @throws CuentaNoEncontradaException si la cuenta no existe o está suprimida
     */
    Usuario actualizar(ActualizarDatosCommand comando);
}
