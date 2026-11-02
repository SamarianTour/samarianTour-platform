package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarAlertasSesgoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.AlertaSesgoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** El aviso de sesgo en el panel del administrador (RET-02, DN-43), con el rol ADMINISTRADOR (DN-05). */
@RestController
@RequestMapping("/admin/alertas-sesgo")
public class AlertasSesgoController {

    private final ConsultarAlertasSesgoUseCase alertas;

    public AlertasSesgoController(ConsultarAlertasSesgoUseCase alertas) {
        this.alertas = alertas;
    }

    @GetMapping
    public List<AlertaSesgoResponse> abiertas() {
        return alertas.abiertas().stream()
                .map(alerta -> new AlertaSesgoResponse(alerta.prestadorId(), alerta.indice(),
                        alerta.porcentajeApariciones(), alerta.porcentajeOfertas(), alerta.apariciones(),
                        alerta.recomendaciones(), alerta.abiertaEn(), alerta.actualizadaEn()))
                .toList();
    }
}
