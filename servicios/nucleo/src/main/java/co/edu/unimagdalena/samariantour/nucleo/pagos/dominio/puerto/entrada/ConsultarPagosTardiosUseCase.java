package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;

import java.util.List;

/** Los pagos tardíos que el administrador reembolsa a mano (RF-50, DN-22). */
public interface ConsultarPagosTardiosUseCase {

    /** Del más reciente al más antiguo. */
    List<PagoTardio> consultar();
}
