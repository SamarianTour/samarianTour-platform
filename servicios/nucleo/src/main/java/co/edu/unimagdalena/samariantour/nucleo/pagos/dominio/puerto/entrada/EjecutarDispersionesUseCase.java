package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

/**
 * Envía al prestador lo que le corresponde de cada pago confirmado, con sus reintentos, y avisa por WhatsApp lo que
 * necesita atención: los pagos tardíos al administrador y las dispersiones fallidas al prestador (RF-50, RF-54, DN-22,
 * DN-24). Lo llama una tarea programada.
 */
public interface EjecutarDispersionesUseCase {

    /** @return cuántas dispersiones intentó */
    int ejecutarPendientes();

    /** @return cuántos avisos envió */
    int enviarAvisos();
}
