package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertasDeZona;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResumenAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.AlertaResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.AlertasZonaResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.ResumenAlertasResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.ZonaConAlertasResponse;

import java.util.List;

/**
 * Convierte las alertas del dominio en las respuestas de la API.
 */
public final class AlertaWebMapper {

    private AlertaWebMapper() {
    }

    public static AlertasZonaResponse aRespuesta(AlertasDeZona alertas) {
        return new AlertasZonaResponse(alertas.zonaId(), alertas.actualizadoEn(), alertas(alertas.alertas()));
    }

    public static ResumenAlertasResponse aRespuesta(ResumenAlertas resumen) {
        return new ResumenAlertasResponse(resumen.actualizadoEn(), resumen.zonas().stream()
                .map(zona -> new ZonaConAlertasResponse(zona.zonaId(), zona.zonaNombre(), alertas(zona.alertas())))
                .toList());
    }

    private static List<AlertaResponse> alertas(List<AlertaClimatica> alertas) {
        return alertas.stream()
                .map(a -> new AlertaResponse(a.id(), a.tipo(), a.nivel(), a.titulo(), a.descripcion(), a.fuente(),
                        a.emitidaEn(), a.vigenteHasta()))
                .toList();
    }
}
