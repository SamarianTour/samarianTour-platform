package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.PagoNoAplicableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CostoTransaccional;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Dispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EfectoEnSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Liquidacion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoIntento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SolicitudCobrable;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.CalculadoraComision;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.PoliticaReintentos;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ProcesarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ComisionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IntentoPagoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.SolicitudesPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Procesa los eventos guardados por el webhook, uno por transacción (RF-50 a RF-53, RF-58, DN-21). Toma el más antiguo
 * con {@code FOR UPDATE SKIP LOCKED}, así las dos réplicas trabajan a la vez sin repetir, y en la misma transacción:
 * <ul>
 *   <li>actualiza el intento y guarda el id de la transacción (RF-52);</li>
 *   <li>aprobado: la solicitud pasa a Pagada (RF-50) y queda la dispersión pendiente con la comisión vigente y el
 *   costo transaccional (RF-53). Si la solicitud ya estaba vencida o cerrada, o ya la había pagado otro intento, se
 *   registra el pago tardío para el reembolso manual; también si el intento ya tenía resultado y llega aprobada otra
 *   transacción con su referencia;</li>
 *   <li>rechazado, anulado o con error: Pago fallido, o Vencida al tercero (RF-58);</li>
 *   <li>un estado no final solo actualiza el intento; un evento sin intento o con otro monto se ignora.</li>
 * </ul>
 * Ante un conflicto de concurrencia se reintenta la transacción completa (ADR-05). Si la solicitud no admite el
 * evento, la transacción se deshace y el evento queda procesado con el código del error, para no repetirlo.
 */
public class ProcesarEventoPagoService implements ProcesarEventoPagoUseCase {

    /** Cuántos eventos procesa como máximo cada vez que corre la tarea. */
    public static final int TAMANO_LOTE = 20;

    private final EventoPasarelaRepositoryPort eventos;
    private final IntentoPagoRepositoryPort intentos;
    private final SolicitudesPort solicitudes;
    private final PagoTardioRepositoryPort tardios;
    private final DispersionRepositoryPort dispersiones;
    private final ComisionRepositoryPort comisiones;
    private final CostoTransaccional costo;
    private final TransaccionPort transaccion;
    private final EjecutorConReintento reintento;
    private final Clock reloj;

    public ProcesarEventoPagoService(EventoPasarelaRepositoryPort eventos, IntentoPagoRepositoryPort intentos,
                                     SolicitudesPort solicitudes, PagoTardioRepositoryPort tardios,
                                     DispersionRepositoryPort dispersiones, ComisionRepositoryPort comisiones,
                                     CostoTransaccional costo, TransaccionPort transaccion,
                                     EjecutorConReintento reintento, Clock reloj) {
        this.eventos = eventos;
        this.intentos = intentos;
        this.solicitudes = solicitudes;
        this.tardios = tardios;
        this.dispersiones = dispersiones;
        this.comisiones = comisiones;
        this.costo = costo;
        this.transaccion = transaccion;
        this.reintento = reintento;
        this.reloj = reloj;
    }

    @Override
    public int procesarPendientes() {
        int procesados = 0;
        while (procesados < TAMANO_LOTE && procesarUno()) {
            procesados++;
        }
        return procesados;
    }

    private boolean procesarUno() {
        AtomicReference<EventoPasarela> tomado = new AtomicReference<>();
        try {
            return reintento.ejecutar(() -> transaccion.ejecutar(() -> {
                Optional<EventoPasarela> evento = eventos.tomarSinProcesar();
                if (evento.isEmpty()) {
                    return false;
                }
                tomado.set(evento.get());
                String resultado = aplicar(evento.get());
                eventos.marcarProcesado(evento.get().procesado(resultado, reloj.instant()));
                return true;
            }));
        } catch (PagoNoAplicableException e) {
            transaccion.ejecutar(() -> {
                eventos.marcarProcesado(tomado.get().procesado(e.codigo(), reloj.instant()));
                return null;
            });
            return true;
        }
    }

    private String aplicar(EventoPasarela evento) {
        Optional<IntentoPago> encontrado = intentos.buscarPorReferencia(evento.referencia());
        if (encontrado.isEmpty()) {
            return "SIN_INTENTO";
        }
        IntentoPago intento = encontrado.get();
        if (intento.montoCentavos() != evento.montoCentavos()) {
            // Una transacción por otro monto no paga la solicitud (RF-49): se deja registrada y no se aplica.
            return "MONTO_DISTINTO";
        }
        Instant ahora = reloj.instant();
        ResultadoIntento resultado = intento.aplicar(evento.estado(), evento.transaccionId(), ahora);
        if (intento.tieneCambios()) {
            intentos.actualizar(intento);
        }
        return switch (resultado) {
            case SIN_CAMBIOS -> "SIN_CAMBIOS";
            case RECHAZADO -> solicitudes.registrarPagoRechazado(intento.solicitudId()).name();
            case APROBADO -> aprobar(intento, ahora);
            case APROBADO_SIN_EFECTO -> {
                tardios.guardar(PagoTardio.de(UUID.randomUUID(), intento, evento.transaccionId(), ahora));
                yield EfectoEnSolicitud.PAGO_TARDIO.name();
            }
        };
    }

    private String aprobar(IntentoPago intento, Instant ahora) {
        EfectoEnSolicitud efecto = solicitudes.registrarPagoAprobado(intento.solicitudId());
        if (efecto != EfectoEnSolicitud.APLICADO) {
            // Vencida o cerrada (PAGO_TARDIO), o ya pagada por otro intento (SIN_CAMBIOS): en los dos casos este dinero
            // no paga nada y el administrador lo reembolsa a mano (RF-50).
            tardios.guardar(PagoTardio.de(UUID.randomUUID(), intento, intento.transaccionId(), ahora));
            return EfectoEnSolicitud.PAGO_TARDIO.name();
        }
        SolicitudCobrable solicitud = solicitudes.consultar(intento.solicitudId(), intento.turistaId())
                .orElseThrow(() -> new IllegalStateException("La solicitud " + intento.solicitudId()
                        + " se acaba de pagar pero no se encuentra"));
        List<ConfiguracionComision> historial = comisiones.historial();
        Liquidacion liquidacion = CalculadoraComision.liquidar(intento.montoCentavos() / 100,
                CalculadoraComision.vigenteEn(ahora, historial).porcentaje(), costo);
        dispersiones.insertar(Dispersion.pendiente(UUID.randomUUID(), intento.solicitudId(), solicitud.prestadorId(),
                liquidacion, ahora));
        return EfectoEnSolicitud.APLICADO.name();
    }

    @Override
    public int abandonarIntentos() {
        return reintento.ejecutar(() -> transaccion.ejecutar(() -> {
            Instant ahora = reloj.instant();
            List<IntentoPago> vencidos = intentos.tomarEnCursoAbiertosAntesDe(
                    ahora.minus(PoliticaReintentos.ABANDONO_INTENTO), TAMANO_LOTE);
            for (IntentoPago intento : vencidos) {
                if (intento.abandonarSiCorresponde(ahora)) {
                    intentos.actualizar(intento);
                    solicitudes.liberarIntentoEnCurso(intento.solicitudId());
                }
            }
            return vencidos.size();
        }));
    }
}
