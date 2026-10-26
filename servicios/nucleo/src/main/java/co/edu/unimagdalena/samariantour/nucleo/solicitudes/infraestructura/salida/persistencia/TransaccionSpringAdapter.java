package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Las transacciones de PostgreSQL con el administrador de transacciones de Spring (ADR-05). */
@Component
public class TransaccionSpringAdapter implements TransaccionPort {

    private final TransactionTemplate transaccion;

    TransaccionSpringAdapter(PlatformTransactionManager transacciones) {
        this.transaccion = new TransactionTemplate(transacciones);
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        return transaccion.execute(estado -> operacion.get());
    }
}
