package co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AlertaSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarAlertasSesgoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.AlertaSesgoRepositoryPort;

import java.util.List;

/** Las alertas de sesgo abiertas, para el panel del administrador (DN-43). */
public class ConsultarAlertasSesgoService implements ConsultarAlertasSesgoUseCase {

    private final AlertaSesgoRepositoryPort alertas;

    public ConsultarAlertasSesgoService(AlertaSesgoRepositoryPort alertas) {
        this.alertas = alertas;
    }

    @Override
    public List<AlertaSesgo> abiertas() {
        return alertas.abiertas();
    }
}
