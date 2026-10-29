package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

/**
 * Lo que hizo una sincronización con el RNT (RF-76, DN-36).
 *
 * @param revisados    los prestadores revisados
 * @param retirados    los que perdieron la habilitación y salieron del catálogo (RF-77)
 * @param restituidos  los que la recuperaron y volvieron al catálogo
 * @param sinRespuesta los que conservaron su último estado porque el RNT no respondió (QAS-03)
 * @param interrumpida si se detuvo porque el RNT dejó de responder
 */
public record SincronizacionRnt(int revisados, int retirados, int restituidos, int sinRespuesta,
                                boolean interrumpida) {
}
