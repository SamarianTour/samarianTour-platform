package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

import java.util.List;

/**
 * Las opiniones aprobadas de una oferta: el resumen de todas y una página, de la más reciente a la más antigua
 * (RF-59). La página tiene la forma de DC-14.
 */
public record OpinionesOfertaResponse(
        ResumenOpinionesResponse resumen,
        List<OpinionPublicaResponse> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        long totalPaginas) {
}
