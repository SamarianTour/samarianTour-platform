package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.util.Objects;

/**
 * Lo que el frontend necesita para abrir el pago en la pasarela (RF-46 a RF-48): la URL del checkout y, para el widget
 * de Wompi, la llave pública y la firma de integridad. El backend nunca ve datos de tarjeta ni de cuenta (RF-47).
 *
 * @param pasarela        SIMULADA o WOMPI
 * @param llavePublica    solo Wompi
 * @param firmaIntegridad solo Wompi: SHA-256 de la referencia, el monto, la moneda y el secreto de integridad
 * @param urlRedireccion  a dónde vuelve el turista; no es prueba del pago (RF-51)
 */
public record SesionCobro(String pasarela, String urlCheckout, String llavePublica, String firmaIntegridad,
                          String urlRedireccion) {

    public SesionCobro {
        Objects.requireNonNull(pasarela, "pasarela");
        Objects.requireNonNull(urlCheckout, "urlCheckout");
    }
}
