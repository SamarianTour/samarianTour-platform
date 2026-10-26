package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.util.Objects;

/**
 * Una solicitud como la ve el operador para verificarla: con el WhatsApp del prestador, al que contacta fuera de la
 * plataforma (RF-37, RF-71) [SUPUESTO] (DN-16). El turista no lo ve.
 *
 * @param whatsappPrestador vacío si el prestador no tiene uno registrado
 */
public record DetalleSolicitudOperador(SolicitudDisponibilidad solicitud, String whatsappPrestador) {

    public DetalleSolicitudOperador {
        Objects.requireNonNull(solicitud, "solicitud");
    }
}
