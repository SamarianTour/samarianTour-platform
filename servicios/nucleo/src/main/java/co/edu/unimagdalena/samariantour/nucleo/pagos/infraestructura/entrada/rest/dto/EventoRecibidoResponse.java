package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto;

/**
 * La respuesta del webhook (DN-21). A la pasarela solo le importa el 200; el cuerpo sirve para el seguimiento.
 *
 * @param duplicado true si el evento ya se había recibido, o no es de una transacción: no se guardó otra vez
 */
public record EventoRecibidoResponse(boolean duplicado) {
}
