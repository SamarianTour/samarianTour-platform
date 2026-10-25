package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertasDeZona;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResumenAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.AlertaResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.AlertasZonaResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.ResumenAlertasResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.ZonaConAlertasResponse;

import java.util.List;

/**
 * Convierte las alertas del dominio en las respuestas de la API, con el título y la descripción en el idioma pedido
 * (DR-21). La fuente no se traduce.
 */
public final class AlertaWebMapper {

    private AlertaWebMapper() {
    }

    public static AlertasZonaResponse aRespuesta(AlertasDeZona alertas, Idioma idioma) {
        return new AlertasZonaResponse(alertas.zonaId(), alertas.actualizadoEn(), alertas(alertas.alertas(), idioma));
    }

    public static ResumenAlertasResponse aRespuesta(ResumenAlertas resumen, Idioma idioma) {
        return new ResumenAlertasResponse(resumen.actualizadoEn(), resumen.zonas().stream()
                .map(zona -> new ZonaConAlertasResponse(zona.zonaId(), zona.zonaNombre(),
                        alertas(zona.alertas(), idioma)))
                .toList());
    }

    private static List<AlertaResponse> alertas(List<AlertaClimatica> alertas, Idioma idioma) {
        return alertas.stream()
                .map(a -> new AlertaResponse(a.id(), a.tipo(), a.nivel(), a.titulo().para(idioma),
                        a.descripcion() == null ? null : a.descripcion().para(idioma), a.fuente(), a.emitidaEn(),
                        a.vigenteHasta()))
                .toList();
    }
}
