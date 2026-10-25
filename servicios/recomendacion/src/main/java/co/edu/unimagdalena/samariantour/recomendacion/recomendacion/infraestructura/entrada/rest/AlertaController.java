package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ConsultarAlertasZonaUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.AlertasZonaResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.ResumenAlertasResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.mapper.AlertaWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alertas activas del destino (RF-28). Públicas, sin rol. Responden desde la caché, nunca llaman a la fuente.
 */
@RestController
public class AlertaController {

    private final ConsultarAlertasZonaUseCase consultarAlertas;

    public AlertaController(ConsultarAlertasZonaUseCase consultarAlertas) {
        this.consultarAlertas = consultarAlertas;
    }

    /** No valida que la zona exista en el catálogo: una zona sin alertas responde la lista vacía. */
    @GetMapping("/zonas/{zonaId}/alertas")
    public AlertasZonaResponse deZona(@PathVariable String zonaId) {
        return AlertaWebMapper.aRespuesta(consultarAlertas.deZona(zonaId));
    }

    @GetMapping("/alertas")
    public ResumenAlertasResponse todas() {
        return AlertaWebMapper.aRespuesta(consultarAlertas.todas());
    }
}
