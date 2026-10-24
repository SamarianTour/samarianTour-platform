package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.List;

/**
 * La respuesta del proveedor de IA tal como llegó, ya leída del JSON y todavía sin validar. Los criterios van como
 * texto porque el modelo puede devolver cualquier cosa; {@code ValidacionRespuestaIA} decide si se acepta (DR-05).
 *
 * @param recomendaciones null si el JSON no trae la lista
 */
public record PropuestaIA(List<Item> recomendaciones) {

    /** Una oferta propuesta; cualquier campo puede llegar null. */
    public record Item(String ofertaId, List<String> criterios, String explicacion) {
    }
}
