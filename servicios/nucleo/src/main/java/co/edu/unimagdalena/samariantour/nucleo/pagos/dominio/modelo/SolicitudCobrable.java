package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que pagos sabe de una solicitud del turista, por {@code solicitudes.api} (DN-17).
 *
 * @param estado     el estado de la solicitud, como texto: pagos no conoce su dominio
 * @param precioBase en pesos; vacío si el operador todavía no la atendió
 * @param venceEn    el fin de la ventana de confirmación y pago; vacío si está cerrada
 */
public record SolicitudCobrable(UUID solicitudId, String estado, Long precioBase, Instant venceEn, UUID prestadorId,
                                String ofertaId) {

    public SolicitudCobrable {
        Objects.requireNonNull(solicitudId, "solicitudId");
        Objects.requireNonNull(estado, "estado");
    }

    /** Solo se paga una solicitud Confirmada o con el pago fallido (RF-46, RF-58). */
    public boolean admitePago() {
        return estado.equals("CONFIRMADA") || estado.equals("PAGO_FALLIDO");
    }
}
