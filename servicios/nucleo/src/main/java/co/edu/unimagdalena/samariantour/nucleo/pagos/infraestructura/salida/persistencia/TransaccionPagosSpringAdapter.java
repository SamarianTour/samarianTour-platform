package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Las transacciones de pagos con el administrador de transacciones de Spring (ADR-05). */
@Component
public class TransaccionPagosSpringAdapter implements TransaccionPort {

    private final TransactionTemplate transaccion;

    TransaccionPagosSpringAdapter(PlatformTransactionManager transacciones) {
        this.transaccion = new TransactionTemplate(transacciones);
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        return transaccion.execute(estado -> operacion.get());
    }
}
