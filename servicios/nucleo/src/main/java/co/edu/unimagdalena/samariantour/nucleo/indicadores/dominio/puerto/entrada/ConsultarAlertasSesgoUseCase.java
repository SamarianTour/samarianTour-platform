package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AlertaSesgo;

import java.util.List;

/** El administrador ve en su panel las alertas de sesgo abiertas (RET-02, DN-43). */
public interface ConsultarAlertasSesgoUseCase {

    /** De la de mayor índice a la de menor. */
    List<AlertaSesgo> abiertas();
}
