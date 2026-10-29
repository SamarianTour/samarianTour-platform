package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ConsultarPagosTardiosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;

import java.util.List;

/** Los pagos tardíos para el reembolso manual (RF-50, DN-22). */
public class ConsultarPagosTardiosService implements ConsultarPagosTardiosUseCase {

    private final PagoTardioRepositoryPort tardios;

    public ConsultarPagosTardiosService(PagoTardioRepositoryPort tardios) {
        this.tardios = tardios;
    }

    @Override
    public List<PagoTardio> consultar() {
        return tardios.todos();
    }
}
