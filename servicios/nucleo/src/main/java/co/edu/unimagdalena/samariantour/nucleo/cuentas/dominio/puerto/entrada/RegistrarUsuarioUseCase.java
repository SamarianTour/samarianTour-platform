package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.PoliticaNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;

/**
 * Un turista o un prestador se registra solo (RF-01, RF-02, RF-03, RF-09 a RF-11). Al turista se le envía el código
 * que verifica su WhatsApp.
 */
public interface RegistrarUsuarioUseCase {

    /**
     * @throws DatoInvalidoException       si un dato no cumple las reglas o no aceptó la política (RF-02, RF-09)
     * @throws PoliticaNoVigenteException  si aceptó una versión que ya no es la vigente (RF-10)
     * @throws CorreoYaRegistradoException si otra cuenta ya tiene el correo (RF-02)
     */
    Usuario registrar(RegistrarUsuarioCommand comando);
}
