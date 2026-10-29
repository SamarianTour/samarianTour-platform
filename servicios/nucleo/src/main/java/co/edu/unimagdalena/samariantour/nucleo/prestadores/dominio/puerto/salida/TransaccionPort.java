package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base, para consultar el RNT antes de abrirla: la consulta puede tardar hasta 3 s y no debe
 * retener una conexión mientras tanto (DN-33).
 */
public interface TransaccionPort {

    /** Ejecuta la operación en una transacción; si lanza, la deshace y relanza. */
    <T> T ejecutar(Supplier<T> operacion);
}
