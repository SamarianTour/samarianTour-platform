package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

/**
 * Lo que hizo una sincronización con el RNT (RF-76, DN-36).
 *
 * @param revisados    los prestadores que tomó
 * @param retirados    los que perdieron la habilitación: el catálogo retira sus ofertas (RF-77)
 * @param restituidos  los que la recuperaron: el catálogo restituye sus ofertas aprobadas
 * @param sinRespuesta los que conservaron su último estado porque el RNT no respondió (QAS-03)
 * @param interrumpida si se detuvo porque el RNT dejó de responder
 */
public record ResultadoSincronizacion(int revisados, int retirados, int restituidos, int sinRespuesta,
                                      boolean interrumpida) {
}
