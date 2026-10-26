package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base, para que la aplicación pueda reintentar el caso de uso completo ante un conflicto de
 * concurrencia (ADR-05): cada intento corre en su propia transacción, porque una que falló por el bloqueo optimista ya
 * no se puede confirmar. Con anotaciones no se puede: el reintento envolvería a la transacción desde fuera del proxy.
 */
public interface TransaccionPort {

    /**
     * Ejecuta la operación en una transacción y la confirma al terminar; si la operación lanza, la deshace y relanza.
     * Si ya hay una transacción abierta, la operación corre en ella.
     */
    <T> T ejecutar(Supplier<T> operacion);
}
