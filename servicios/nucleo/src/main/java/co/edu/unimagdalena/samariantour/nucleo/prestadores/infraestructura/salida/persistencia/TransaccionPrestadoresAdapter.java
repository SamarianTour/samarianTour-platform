package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.TransaccionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Las transacciones de los prestadores, con el gestor de transacciones de Spring. */
@Component
public class TransaccionPrestadoresAdapter implements TransaccionPort {

    private final TransactionTemplate transaccion;

    TransaccionPrestadoresAdapter(PlatformTransactionManager transacciones) {
        this.transaccion = new TransactionTemplate(transacciones);
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        return transaccion.execute(estado -> operacion.get());
    }
}
