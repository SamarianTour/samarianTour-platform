package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Dispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoPasarela;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.MotivoPagoTardio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OrdenDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.TipoAvisoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.EjecutarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.AvisoPagoPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ContactosPagosPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.CuentasCobroPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Las dispersiones y los avisos de pagos (RF-50, RF-54, RF-74, RF-75, DN-21, DN-22, DN-24). Toma cada una con
 * {@code FOR UPDATE SKIP LOCKED}, en su propia transacción, así dos réplicas nunca envían la misma y un error con una
 * no detiene a las demás:
 * <ul>
 *   <li>cada dispersión pendiente va a la cuenta vigente del prestador en ese momento (RF-74); si falla, se reintenta
 *   tres veces con espera creciente y luego queda fallida (RF-54). Un error inesperado cuenta como un fallo más;</li>
 *   <li>los pagos tardíos, los aprobados por otro monto y los eventos fallidos se avisan al administrador, y las
 *   dispersiones fallidas al prestador, por WhatsApp, después de confirmar el cambio. Un aviso que no sale espera su
 *   turno detrás de los demás y se reintenta en la siguiente vuelta.</li>
 * </ul>
 * La cuenta nunca sale completa: a la dispersión y al aviso solo llegan sus últimos 4 dígitos (RF-75).
 */
public class EjecutarDispersionesService implements EjecutarDispersionesUseCase {

    public static final int TAMANO_LOTE = 10;

    private final DispersionRepositoryPort dispersiones;
    private final PagoTardioRepositoryPort tardios;
    private final EventoPasarelaRepositoryPort eventos;
    private final CuentasCobroPort cuentas;
    private final ContactosPagosPort contactos;
    private final PaymentGatewayPort pasarela;
    private final AvisoPagoPort avisos;
    private final TransaccionPort transaccion;
    private final Duration esperaBase;
    private final Clock reloj;

    /** @param esperaBase la espera antes del primer reintento de una dispersión; se duplica en cada uno */
    public EjecutarDispersionesService(DispersionRepositoryPort dispersiones, PagoTardioRepositoryPort tardios,
                                       EventoPasarelaRepositoryPort eventos, CuentasCobroPort cuentas,
                                       ContactosPagosPort contactos, PaymentGatewayPort pasarela,
                                       AvisoPagoPort avisos, TransaccionPort transaccion, Duration esperaBase,
                                       Clock reloj) {
        this.dispersiones = dispersiones;
        this.tardios = tardios;
        this.eventos = eventos;
        this.cuentas = cuentas;
        this.contactos = contactos;
        this.pasarela = pasarela;
        this.avisos = avisos;
        this.transaccion = transaccion;
        this.esperaBase = esperaBase;
        this.reloj = reloj;
    }

    /** @return las dispersiones que intentó, con éxito o no */
    @Override
    public int ejecutarPendientes() {
        int intentadas = 0;
        while (intentadas < TAMANO_LOTE && ejecutarUna()) {
            intentadas++;
        }
        return intentadas;
    }

    private boolean ejecutarUna() {
        AtomicReference<Dispersion> tomada = new AtomicReference<>();
        try {
            return transaccion.ejecutar(() -> {
                Optional<Dispersion> dispersion = dispersiones.tomarPendiente(reloj.instant());
                dispersion.ifPresent(pendiente -> {
                    tomada.set(pendiente);
                    ejecutar(pendiente);
                });
                return dispersion.isPresent();
            });
        } catch (RuntimeException e) {
            if (tomada.get() == null) {
                throw e;
            }
            registrarError(tomada.get().id(), e);
            return true;
        }
    }

    private void ejecutar(Dispersion dispersion) {
        Optional<CuentaCobro> cuenta = cuentas.cuentaVigente(dispersion.prestadorId());
        if (cuenta.isEmpty()) {
            dispersion.registrarFallo("El prestador no tiene una cuenta de cobro registrada", null, reloj.instant(),
                    esperaBase);
        } else {
            ResultadoDispersion resultado = enviar(dispersion, cuenta.get());
            Instant ahora = reloj.instant();
            if (resultado.exitosa()) {
                dispersion.registrarExito(cuenta.get().enmascarada(), resultado.idPasarela(), ahora);
            } else {
                dispersion.registrarFallo(resultado.causa(), cuenta.get().enmascarada(), ahora, esperaBase);
            }
        }
        dispersiones.actualizar(dispersion);
    }

    private ResultadoDispersion enviar(Dispersion dispersion, CuentaCobro cuenta) {
        try {
            return pasarela.dispersar(new OrdenDispersion(dispersion.id(), dispersion.prestadorId(),
                    dispersion.montoNeto(), cuenta));
        } catch (RuntimeException e) {
            return ResultadoDispersion.fallo("La pasarela no respondió: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Un error inesperado al ejecutarla (por ejemplo, al leer la cuenta) cuenta como un fallo más, con su espera, en
     * una transacción aparte: la de la ejecución ya se deshizo.
     */
    private void registrarError(UUID dispersionId, RuntimeException error) {
        transaccion.ejecutar(() -> {
            dispersiones.pendientePorId(dispersionId).ifPresent(dispersion -> {
                dispersion.registrarFallo("Error inesperado: " + error.getClass().getSimpleName(), null,
                        reloj.instant(), esperaBase);
                dispersiones.actualizar(dispersion);
            });
            return null;
        });
    }

    @Override
    public int enviarAvisos() {
        Instant inicio = reloj.instant();
        int tardiosAvisados = avisarCada(() -> tardios.tomarSinAviso(inicio),
                pago -> contactos.whatsappAdministrador(), EjecutarDispersionesService::tipo,
                EjecutarDispersionesService::parametros,
                pago -> tardios.marcarAvisado(pago.avisado(reloj.instant())),
                pago -> tardios.marcarAvisoIntentado(pago.id(), reloj.instant()));
        int dispersionesAvisadas = avisarCada(() -> dispersiones.tomarFallidaSinAviso(inicio),
                dispersion -> contactos.whatsappPrestador(dispersion.prestadorId()),
                dispersion -> TipoAvisoPago.DISPERSION_FALLIDA, EjecutarDispersionesService::parametros,
                dispersion -> {
                    dispersion.registrarAviso(reloj.instant());
                    dispersiones.actualizar(dispersion);
                },
                dispersion -> dispersiones.marcarAvisoIntentado(dispersion.id(), reloj.instant()));
        int eventosAvisados = avisarCada(() -> eventos.tomarFallidoSinAviso(inicio),
                evento -> contactos.whatsappAdministrador(), evento -> TipoAvisoPago.EVENTO_FALLIDO,
                EjecutarDispersionesService::parametros,
                evento -> eventos.marcarAvisado(evento.avisado(reloj.instant())),
                evento -> eventos.marcarAvisoIntentado(evento.id(), reloj.instant()));
        return tardiosAvisados + dispersionesAvisadas + eventosAvisados;
    }

    /**
     * Avisa uno por uno, cada uno en su transacción. Uno que no se puede avisar (sin WhatsApp, el envío falla o un error
     * inesperado) queda anotado como intentado y pasa al final de la fila: no se vuelve a tomar en esta vuelta ni queda
     * de primero en la siguiente.
     *
     * @param tomar     el siguiente sin avisar que no se intentó en esta vuelta
     * @param avisado   lo marca como avisado
     * @param intentado anota que se intentó y no se pudo
     * @return cuántos se avisaron
     */
    private <T> int avisarCada(Supplier<Optional<T>> tomar, Function<T, Optional<String>> destinatario,
                               Function<T, TipoAvisoPago> tipo, Function<T, Map<String, String>> parametros,
                               Consumer<T> avisado, Consumer<T> intentado) {
        int enviados = 0;
        for (int revisados = 0; revisados < TAMANO_LOTE; revisados++) {
            AtomicReference<T> tomadoRef = new AtomicReference<>();
            Optional<Boolean> enviado;
            try {
                enviado = transaccion.ejecutar(() -> tomar.get().map(tomado -> {
                    tomadoRef.set(tomado);
                    Optional<String> whatsapp = destinatario.apply(tomado);
                    if (whatsapp.isPresent() && avisar(whatsapp.get(), tipo.apply(tomado), parametros.apply(tomado))) {
                        avisado.accept(tomado);
                        return true;
                    }
                    intentado.accept(tomado);
                    return false;
                }));
            } catch (RuntimeException e) {
                if (tomadoRef.get() == null) {
                    throw e;
                }
                T tomado = tomadoRef.get();
                transaccion.ejecutar(() -> {
                    intentado.accept(tomado);
                    return null;
                });
                continue;
            }
            if (enviado.isEmpty()) {
                break;
            }
            if (enviado.get()) {
                enviados++;
            }
        }
        return enviados;
    }

    private boolean avisar(String whatsapp, TipoAvisoPago tipo, Map<String, String> parametros) {
        try {
            avisos.enviar(whatsapp, tipo, parametros);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static TipoAvisoPago tipo(PagoTardio pago) {
        return pago.motivo() == MotivoPagoTardio.MONTO_DISTINTO ? TipoAvisoPago.MONTO_DISTINTO
                : TipoAvisoPago.PAGO_TARDIO;
    }

    private static Map<String, String> parametros(PagoTardio pago) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("solicitudId", pago.solicitudId().toString());
        parametros.put("referencia", pago.referencia());
        parametros.put("transaccionId", pago.transaccionId());
        parametros.put("monto", Long.toString(pago.montoCentavos() / 100));
        return parametros;
    }

    private static Map<String, String> parametros(Dispersion dispersion) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("solicitudId", dispersion.solicitudId().toString());
        parametros.put("montoNeto", Long.toString(dispersion.montoNeto()));
        if (dispersion.cuentaDestino() != null) {
            parametros.put("cuenta", dispersion.cuentaDestino());
        }
        parametros.put("causa", dispersion.ultimaCausa());
        return parametros;
    }

    /** Lo que el administrador necesita para buscarlo en la pasarela; el error no va, porque es técnico. */
    private static Map<String, String> parametros(EventoPasarela evento) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("referencia", evento.referencia());
        parametros.put("transaccionId", evento.transaccionId());
        parametros.put("estado", evento.estado().name());
        parametros.put("monto", Long.toString(evento.montoCentavos() / 100));
        parametros.put("intentos", Integer.toString(evento.intentosProcesamiento()));
        return parametros;
    }
}
