package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.IntentosAgotadosException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.SolicitudNoCobrableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionGenerada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SolicitudCobrable;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.PoliticaReintentos;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.GenerarSesionCobroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IntentoPagoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.SolicitudesPort;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Abre el pago de una solicitud (RF-46 a RF-49, RF-55, RF-58), en la transacción de su clave de idempotencia (DN-19):
 * <ol>
 *   <li>la solicitud debe ser del turista (si no, 404) y estar Confirmada o con el pago fallido (si no, 409);</li>
 *   <li>si ya hay un intento en curso, devuelve ese mismo sin abrir otro;</li>
 *   <li>si no, abre el siguiente, salvo que ya haya tres con resultado de la pasarela (RF-58), y marca la solicitud
 *   con un intento en curso, que aplaza su vencimiento hasta 30 min (RF-55);</li>
 *   <li>la pasarela arma lo que el frontend necesita para abrir el pago; no llama a la red.</li>
 * </ol>
 */
public class GenerarSesionCobroService implements GenerarSesionCobroUseCase {

    private final SolicitudesPort solicitudes;
    private final IntentoPagoRepositoryPort intentos;
    private final PaymentGatewayPort pasarela;
    private final EjecucionIdempotente idempotente;
    private final Clock reloj;

    public GenerarSesionCobroService(SolicitudesPort solicitudes, IntentoPagoRepositoryPort intentos,
                                     PaymentGatewayPort pasarela, EjecucionIdempotente idempotente, Clock reloj) {
        this.solicitudes = solicitudes;
        this.intentos = intentos;
        this.pasarela = pasarela;
        this.idempotente = idempotente;
        this.reloj = reloj;
    }

    @Override
    public ResultadoIdempotente generar(UUID solicitudId, UUID turistaId, PeticionIdempotente peticion,
                                        Function<SesionGenerada, RespuestaGuardada> respuesta) {
        return idempotente.ejecutar(peticion, () -> respuesta.apply(abrir(solicitudId, turistaId)));
    }

    private SesionGenerada abrir(UUID solicitudId, UUID turistaId) {
        SolicitudCobrable solicitud = solicitudes.consultar(solicitudId, turistaId)
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));
        if (!solicitud.admitePago()) {
            throw new SolicitudNoCobrableException(solicitud.estado());
        }
        Optional<IntentoPago> enCurso = intentos.enCursoDe(solicitudId);
        IntentoPago intento = enCurso.isPresent() ? enCurso.get() : abrirSiguiente(solicitud, turistaId);
        return new SesionGenerada(intento, pasarela.crearSesion(intento));
    }

    private IntentoPago abrirSiguiente(SolicitudCobrable solicitud, UUID turistaId) {
        List<IntentoPago> previos = intentos.deLaSolicitud(solicitud.solicitudId());
        long conResultado = previos.stream().filter(previo -> previo.estado().tieneResultado()).count();
        if (!PoliticaReintentos.admiteOtroIntento(conResultado)) {
            throw new IntentosAgotadosException();
        }
        IntentoPago intento = IntentoPago.nuevo(UUID.randomUUID(), solicitud.solicitudId(), turistaId,
                previos.size() + 1, solicitud.precioBase(), reloj.instant());
        intentos.insertar(intento);
        solicitudes.marcarIntentoEnCurso(solicitud.solicitudId());
        return intento;
    }

    @Override
    public List<IntentoPago> intentos(UUID solicitudId, UUID turistaId) {
        if (solicitudes.consultar(solicitudId, turistaId).isEmpty()) {
            throw new SolicitudNoEncontradaException(solicitudId);
        }
        return intentos.deLaSolicitud(solicitudId);
    }
}
