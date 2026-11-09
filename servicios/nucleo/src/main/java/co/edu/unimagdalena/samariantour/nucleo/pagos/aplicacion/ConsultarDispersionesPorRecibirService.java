package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ConsultarDispersionesPorRecibirUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;

import java.util.UUID;

/** Cuenta en la base las dispersiones pendientes y fallidas del prestador (DN-45). */
public class ConsultarDispersionesPorRecibirService implements ConsultarDispersionesPorRecibirUseCase {

    private final DispersionRepositoryPort dispersiones;

    public ConsultarDispersionesPorRecibirService(DispersionRepositoryPort dispersiones) {
        this.dispersiones = dispersiones;
    }

    @Override
    public long porRecibir(UUID prestadorId) {
        return dispersiones.porRecibir(prestadorId);
    }
}
