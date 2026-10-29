package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaCobroNoRegistradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoCuenta;

import java.util.UUID;

/**
 * El prestador registra la cuenta bancaria o la billetera donde recibe la dispersión (RF-74, RF-75). Se registra una
 * vez, no en cada pago; si la cambia, la anterior queda en el historial y el cambio en la auditoría (DN-34).
 */
public interface RegistrarCuentaCobroUseCase {

    /**
     * Registrar otra vez los mismos datos no cambia nada.
     *
     * @throws DatoInvalidoException          si un dato no cumple las reglas
     * @throws PrestadorNoRegistradoException si todavía no registró su RNT
     */
    CuentaCobro registrar(UUID prestadorId, TipoCuenta tipo, String entidad, String numero, String titular);

    /** @throws CuentaCobroNoRegistradaException si todavía no registró una */
    CuentaCobro vigente(UUID prestadorId);
}
