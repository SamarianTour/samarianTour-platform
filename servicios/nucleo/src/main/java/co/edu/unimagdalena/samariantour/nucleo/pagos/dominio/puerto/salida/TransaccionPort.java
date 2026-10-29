package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base, como en las solicitudes (DN-11): cada reintento por un conflicto de concurrencia necesita
 * la suya, y con {@code @Transactional} quedaría dentro de la que falló.
 */
public interface TransaccionPort {

    <T> T ejecutar(Supplier<T> operacion);
}
