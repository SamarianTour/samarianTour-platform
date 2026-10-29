package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.TipoAvisoPago;

import java.util.Map;

/** Envía un aviso de pagos por WhatsApp (DN-22, DN-24). Si falla, lanza la excepción y se reintenta después. */
public interface AvisoPagoPort {

    void enviar(String whatsapp, TipoAvisoPago tipo, Map<String, String> parametros);
}
