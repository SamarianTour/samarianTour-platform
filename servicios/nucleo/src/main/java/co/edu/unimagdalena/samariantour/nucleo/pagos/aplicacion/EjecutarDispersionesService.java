package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Dispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OrdenDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.TipoAvisoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.EjecutarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.AvisoPagoPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ContactosPagosPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.CuentasCobroPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Las dispersiones y los avisos de pagos (RF-50, RF-54, RF-74, RF-75, DN-22, DN-24). Toma su lote con
 * {@code FOR UPDATE SKIP LOCKED}, así dos réplicas nunca envían la misma:
 * <ul>
 *   <li>cada dispersión pendiente va a la cuenta vigente del prestador en ese momento (RF-74); si falla, se reintenta
 *   tres veces con espera creciente y luego queda fallida (RF-54);</li>
 *   <li>los pagos tardíos se avisan al administrador y las dispersiones fallidas al prestador, por WhatsApp, después de
 *   confirmar el cambio: un aviso que no sale se reintenta en la siguiente vuelta.</li>
 * </ul>
 * La cuenta nunca sale completa: a la dispersión y al aviso solo llegan sus últimos 4 dígitos (RF-75).
 */
public class EjecutarDispersionesService implements EjecutarDispersionesUseCase {

    public static final int TAMANO_LOTE = 10;

    private final DispersionRepositoryPort dispersiones;
    private final PagoTardioRepositoryPort tardios;
    private final CuentasCobroPort cuentas;
    private final ContactosPagosPort contactos;
    private final PaymentGatewayPort pasarela;
    private final AvisoPagoPort avisos;
    private final TransaccionPort transaccion;
    private final Duration esperaBase;
    private final Clock reloj;

    /** @param esperaBase la espera antes del primer reintento de una dispersión; se duplica en cada uno */
    public EjecutarDispersionesService(DispersionRepositoryPort dispersiones, PagoTardioRepositoryPort tardios,
                                       CuentasCobroPort cuentas, ContactosPagosPort contactos,
                                       PaymentGatewayPort pasarela, AvisoPagoPort avisos, TransaccionPort transaccion,
                                       Duration esperaBase, Clock reloj) {
        this.dispersiones = dispersiones;
        this.tardios = tardios;
        this.cuentas = cuentas;
        this.contactos = contactos;
        this.pasarela = pasarela;
        this.avisos = avisos;
        this.transaccion = transaccion;
        this.esperaBase = esperaBase;
        this.reloj = reloj;
    }

    @Override
    public int ejecutarPendientes() {
        return transaccion.ejecutar(() -> {
            List<Dispersion> lote = dispersiones.tomarPendientes(reloj.instant(), TAMANO_LOTE);
            lote.forEach(this::ejecutar);
            return lote.size();
        });
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

    @Override
    public int enviarAvisos() {
        return transaccion.ejecutar(() -> {
            int enviados = 0;
            Optional<String> administrador = contactos.whatsappAdministrador();
            for (PagoTardio pago : tardios.tomarSinAviso(TAMANO_LOTE)) {
                if (administrador.isPresent() && avisar(administrador.get(), TipoAvisoPago.PAGO_TARDIO,
                        parametros(pago))) {
                    tardios.marcarAvisado(pago.avisado(reloj.instant()));
                    enviados++;
                }
            }
            for (Dispersion dispersion : dispersiones.tomarFallidasSinAviso(TAMANO_LOTE)) {
                Optional<String> prestador = contactos.whatsappPrestador(dispersion.prestadorId());
                if (prestador.isPresent() && avisar(prestador.get(), TipoAvisoPago.DISPERSION_FALLIDA,
                        parametros(dispersion))) {
                    dispersion.registrarAviso(reloj.instant());
                    dispersiones.actualizar(dispersion);
                    enviados++;
                }
            }
            return enviados;
        });
    }

    private boolean avisar(String whatsapp, TipoAvisoPago tipo, Map<String, String> parametros) {
        try {
            avisos.enviar(whatsapp, tipo, parametros);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
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
}
