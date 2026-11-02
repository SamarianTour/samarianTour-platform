package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaInternaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaPropiaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorConPrestadoresException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;

import java.util.List;
import java.util.UUID;

/** El administrador gestiona las cuentas de administradores, operadores y gestores del destino (RF-81, DN-40). */
public interface GestionarCuentasInternasUseCase {

    /**
     * @throws CorreoYaRegistradoException si otra cuenta tiene el correo
     * @throws CorreoNoDisponibleException si la invitación no se pudo enviar; no se crea nada
     */
    CuentaInterna crear(CrearCuentaInternaCommand comando);

    List<CuentaInterna> consultar();

    /**
     * @param administradorId el de la identidad, que no puede desactivarse a sí mismo
     * @throws CuentaPropiaException              si es su propia cuenta
     * @throws CuentaInternaNoEncontradaException si no existe o no es interna
     * @throws OperadorConPrestadoresException    si es un operador con prestadores asignados
     */
    CuentaInterna desactivar(UUID usuarioId, UUID administradorId);

    /** @throws CuentaInternaNoEncontradaException si no existe o no es interna */
    CuentaInterna reactivar(UUID usuarioId);
}
