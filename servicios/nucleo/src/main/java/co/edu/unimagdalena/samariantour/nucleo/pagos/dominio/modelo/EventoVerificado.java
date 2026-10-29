package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.util.Objects;

/**
 * Lo que la pasarela verificó de un evento del webhook: la firma es válida y estos son sus datos (RF-50). Lo arma el
 * adaptador de la pasarela, que es el único que conoce su formato.
 *
 * @param cuerpo el JSON recibido, sin cambios, para guardarlo
 */
public record EventoVerificado(String transaccionId, String referencia, EstadoTransaccion estado, long montoCentavos,
                               String cuerpo) {

    public EventoVerificado {
        Objects.requireNonNull(transaccionId, "transaccionId");
        Objects.requireNonNull(referencia, "referencia");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(cuerpo, "cuerpo");
    }

    /** El id de la transacción y su estado [SUPUESTO] (DN-21): el mismo par es el mismo evento. */
    public String claveUnica() {
        return transaccionId + ":" + estado.name();
    }
}
