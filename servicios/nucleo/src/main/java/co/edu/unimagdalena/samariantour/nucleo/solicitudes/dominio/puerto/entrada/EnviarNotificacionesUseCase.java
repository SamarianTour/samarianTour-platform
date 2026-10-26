package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

/** Envía por WhatsApp las notificaciones pendientes de las solicitudes (RF-41, RF-44, RF-45, RF-56, RF-57, DN-13). */
public interface EnviarNotificacionesUseCase {

    /** Envía un lote en una transacción; devuelve cuántas tomó (0 si no había ninguna por enviar). */
    int enviarLote();
}
