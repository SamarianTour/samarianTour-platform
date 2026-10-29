package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;

import java.util.UUID;

/**
 * El turista pide eliminar sus datos personales y se cumple de inmediato (RF-13, Ley 1581, DN-31): la cuenta queda
 * anonimizada, sus sesiones cerradas y el catálogo recibe el aviso para borrar sus opiniones (DC-45). Queda la
 * constancia de la petición.
 */
public interface SolicitarSupresionUseCase {

    /**
     * @param contrasena la actual, que confirma que el titular lo pide
     * @throws ContrasenaIncorrectaException si la contraseña no es correcta; cuenta para el bloqueo de RF-05
     * @throws CuentaBloqueadaException      si la cuenta está bloqueada por intentos fallidos
     * @throws CuentaNoEncontradaException   si no hay un turista activo con ese identificador
     */
    void suprimir(UUID turistaId, String contrasena);
}
