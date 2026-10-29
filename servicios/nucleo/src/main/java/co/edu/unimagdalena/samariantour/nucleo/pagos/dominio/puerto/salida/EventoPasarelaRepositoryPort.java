package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;

import java.util.Optional;

/** Los eventos de la pasarela en PostgreSQL, únicos por su clave (DN-21). */
public interface EventoPasarelaRepositoryPort {

    /** @return false si ya había uno con la misma clave: un duplicado no se guarda otra vez */
    boolean guardarSiNuevo(EventoPasarela evento);

    /**
     * El evento sin procesar más antiguo, bloqueado hasta el final de la transacción; los que otra transacción tiene
     * bloqueados se saltan, así dos réplicas nunca procesan el mismo.
     */
    Optional<EventoPasarela> tomarSinProcesar();

    void marcarProcesado(EventoPasarela evento);
}
