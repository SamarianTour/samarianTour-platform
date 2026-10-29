package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;

import java.util.UUID;

/** El turista confirma que el WhatsApp es suyo con un código de un solo uso (RF-03, RF-08). */
public interface VerificarWhatsAppUseCase {

    /**
     * Envía un código nuevo; el anterior deja de servir. Si el número ya está verificado no envía nada.
     *
     * @throws CuentaNoEncontradaException si no hay un turista con ese identificador
     */
    void enviarCodigo(UUID turistaId);

    /**
     * Verificar un número ya verificado no cambia nada.
     *
     * @throws CodigoNoVigenteException    si el último código venció, ya se usó o agotó sus intentos
     * @throws CodigoIncorrectoException   si el código no coincide; el intento queda contado
     * @throws CuentaNoEncontradaException si no hay un turista con ese identificador
     */
    Turista verificar(UUID turistaId, String codigo);
}
