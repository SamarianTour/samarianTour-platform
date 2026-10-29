package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoVerificado;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.RecibirEventoPasarelaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Recibe un evento del webhook (RF-50, RT-07, DN-21): la pasarela verifica la firma; si no es válida, nada se guarda.
 * Si es válida, el evento se guarda una sola vez (un duplicado no se vuelve a guardar) y el procesamiento queda para la
 * tarea programada: así la respuesta a la pasarela sale de inmediato y nunca depende de las solicitudes.
 */
public class RecibirEventoPasarelaService implements RecibirEventoPasarelaUseCase {

    private final PaymentGatewayPort pasarela;
    private final EventoPasarelaRepositoryPort eventos;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public RecibirEventoPasarelaService(PaymentGatewayPort pasarela, EventoPasarelaRepositoryPort eventos,
                                        TransaccionPort transaccion, Clock reloj) {
        this.pasarela = pasarela;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public boolean recibir(String cuerpo, String firmaCabecera) {
        Optional<EventoVerificado> verificado = pasarela.verificarEvento(cuerpo, firmaCabecera);
        if (verificado.isEmpty()) {
            return false;
        }
        return transaccion.ejecutar(() -> eventos.guardarSiNuevo(
                EventoPasarela.recibido(UUID.randomUUID(), verificado.get(), reloj.instant())));
    }
}
