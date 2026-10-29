package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Lo que el frontend necesita para abrir el pago en la pasarela (RF-46 a RF-48): redirigir a la URL, o abrir el widget
 * de Wompi con la llave pública y la firma de integridad.
 *
 * @param pasarela        SIMULADA o WOMPI
 * @param llavePublica    solo Wompi
 * @param firmaIntegridad solo Wompi
 * @param urlRedireccion  a dónde vuelve el turista; no es prueba del pago (RF-51)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckoutResponse(String pasarela, String url, String llavePublica, String firmaIntegridad,
                               String urlRedireccion) {
}
