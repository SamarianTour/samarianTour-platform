package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoAutenticoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoVerificado;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OrdenDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionCobro;

import java.util.Optional;

/**
 * La pasarela de pago (diagrama de paquetes, módulo 03; QAS-06, DN-18). Hay dos adaptadores y se elige uno por
 * configuración ({@code nucleo.pagos.pasarela}): la simulada del prototipo y Wompi. El dominio no sabe cuál es.
 */
public interface PaymentGatewayPort {

    /**
     * Lo que el frontend necesita para abrir el pago del intento (RF-46 a RF-48). No llama a la red: es el mismo para
     * el mismo intento, así que se puede volver a entregar.
     */
    SesionCobro crearSesion(IntentoPago intento);

    /**
     * Verifica que el evento del webhook lo firmó la pasarela y lee sus datos (RF-50).
     *
     * @return vacío si es auténtico pero no es de una transacción (la pasarela también avisa otras cosas): se responde
     *         200 y no se guarda
     * @param cuerpo        el JSON recibido, sin cambios
     * @param firmaCabecera el checksum que llegó en la cabecera, si llegó; el del cuerpo manda
     * @throws EventoNoAutenticoException si la firma no es válida o el evento no se puede leer
     */
    Optional<EventoVerificado> verificarEvento(String cuerpo, String firmaCabecera);

    /** Ordena la transferencia al prestador (RF-54). Una falla de la pasarela se devuelve, no se lanza. */
    ResultadoDispersion dispersar(OrdenDispersion orden);
}
