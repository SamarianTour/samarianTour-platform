package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.WhatsappNoConfiguradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;

import java.util.UUID;

/**
 * El canal de contacto del prestador, para la notificación de una solicitud Pagada (RF-57, RF-71). Nunca es público.
 * Se verifica con un código de 6 dígitos, como el del turista (DN-33); sin verificar no se le notifica.
 */
public interface ConfigurarWhatsAppPrestadorUseCase {

    /**
     * Guarda el número y, si es nuevo, le envía un código.
     *
     * @throws DatoInvalidoException          si no tiene el indicativo del país
     * @throws PrestadorNoRegistradoException si todavía no registró su RNT
     */
    Prestador configurar(UUID prestadorId, String whatsapp);

    /**
     * Envía otro código; el anterior deja de servir. Con el número ya verificado no envía nada.
     *
     * @throws WhatsappNoConfiguradoException si todavía no configuró su número
     */
    void enviarCodigo(UUID prestadorId);

    /**
     * @throws CodigoNoVigenteException  si el código venció, se usó o agotó sus intentos
     * @throws CodigoIncorrectoException si no coincide; el intento queda contado
     */
    Prestador verificar(UUID prestadorId, String codigo);
}
