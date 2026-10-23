package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

/**
 * Una foto con la dirección de cada tamaño (DC-39). Las direcciones son rutas del catálogo, no de MinIO, y nunca
 * cambian: la misma foto siempre tiene las mismas.
 *
 * @param id           identificador de la foto; es el que se usa para quitarla
 * @param url          la foto para la galería, de hasta 1280 px por lado
 * @param urlMiniatura la foto para las tarjetas, de hasta 400 px por lado
 */
public record FotoResponse(String id, String url, String urlMiniatura) {
}
