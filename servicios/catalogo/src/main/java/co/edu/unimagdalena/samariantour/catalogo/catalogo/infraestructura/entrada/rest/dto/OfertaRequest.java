package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contenido que el prestador envía al publicar o editar una oferta. {@code tipo} decide qué campos aplican;
 * los que no corresponden a ese tipo se ignoran. Los valores enumerados y las horas llegan como texto
 * para que un valor desconocido responda DATO_INVALIDO con los valores permitidos.
 *
 * @param tipo                 ACTIVIDAD, PAQUETE, ALOJAMIENTO o RESTAURANTE
 * @param fotos                identificadores de fotos que el prestador subió para sus ofertas, de 1 a 6, en el orden
 *                             en que se muestran; la primera es la portada (DC-47)
 * @param tipoActividad        solo ACTIVIDAD
 * @param nivelEsfuerzo        solo ACTIVIDAD
 * @param accesibleSillaRuedas ACTIVIDAD, ALOJAMIENTO y RESTAURANTE; si falta, se toma como false
 * @param horariosSalida       ACTIVIDAD y PAQUETE; horas en formato HH:mm
 * @param puntoEncuentro       solo ACTIVIDAD
 * @param duracionMinutos      ACTIVIDAD y PAQUETE
 * @param modalidad            solo ACTIVIDAD
 * @param idiomasAtencion      solo ACTIVIDAD
 * @param actividadIds         solo PAQUETE
 * @param tipoUnidad           solo ALOJAMIENTO
 */
public record OfertaRequest(
        String tipo,
        String nombre,
        String descripcion,
        BigDecimal precioReferencia,
        String horarios,
        String zonaId,
        CoordenadaDto ubicacion,
        String comoLlegar,
        Integer capacidadMaxima,
        List<String> fotos,
        String tipoActividad,
        String nivelEsfuerzo,
        Boolean accesibleSillaRuedas,
        List<String> horariosSalida,
        String puntoEncuentro,
        Integer duracionMinutos,
        String modalidad,
        List<String> idiomasAtencion,
        List<String> actividadIds,
        String tipoUnidad) {
}
