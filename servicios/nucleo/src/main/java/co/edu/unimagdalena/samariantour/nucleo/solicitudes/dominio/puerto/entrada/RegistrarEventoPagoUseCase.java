package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DatosCobro;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoEventoPago;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que el módulo de pagos le hace a una solicitud (RF-50, RF-55, RF-58, DN-17). Los métodos que la cambian corren
 * dentro de la transacción de pagos: el pago aprobado y el paso a Pagada se confirman juntos (documento de diseño:
 * PagoService invoca a SolicitudService en la misma transacción).
 */
public interface RegistrarEventoPagoUseCase {

    /**
     * @return vacío si no existe, no es de ese turista o no está Confirmada ni con el pago fallido
     */
    Optional<DatosCobro> consultarParaCobro(UUID solicitudId, UUID turistaId);

    /** Empieza un intento en la pasarela: el vencimiento espera su resultado hasta 30 min (RF-55). */
    ResultadoEventoPago marcarIntentoEnCurso(UUID solicitudId);

    /** El webhook confirmó el pago (RF-50): Pagada, o pago tardío si ya venció. */
    ResultadoEventoPago registrarPagoAprobado(UUID solicitudId);

    /** El webhook informó un rechazo (RF-58): Pago fallido, o Vencida al tercero o fuera de la ventana. */
    ResultadoEventoPago registrarPagoRechazado(UUID solicitudId);
}
