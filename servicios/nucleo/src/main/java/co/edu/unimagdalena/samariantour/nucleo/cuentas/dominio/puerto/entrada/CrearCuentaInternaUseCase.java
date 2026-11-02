package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;

/** El administrador crea la cuenta de un administrador, un operador o un gestor del destino (RF-81). */
public interface CrearCuentaInternaUseCase {

    /**
     * Nace sin contraseña y le llega al correo el enlace para ponerla (DN-40).
     *
     * @throws CorreoYaRegistradoException si otra cuenta tiene el correo (RF-02)
     * @throws CorreoNoDisponibleException si el correo no se pudo enviar; no se crea nada
     */
    Usuario crear(CrearCuentaInternaCommand comando);
}
