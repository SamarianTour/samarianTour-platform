package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.PrestadorSinOperadorException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.WhatsappNoVerificadoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OfertaSolicitable;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica.ReglaNoDuplicado;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.EnviarSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.EnviarSolicitudCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ContactoPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Function;

/**
 * Crea una solicitud (RF-30 a RF-32, RF-34), dentro de la transacción de su clave de idempotencia (DN-12):
 * <ol>
 *   <li>el turista debe tener WhatsApp verificado (RF-30);</li>
 *   <li>la oferta debe estar visible en el catálogo: es la única llamada de red, con corte de circuito (ADR-04); si el
 *   catálogo no responde no se crea nada y el turista puede reintentar con la misma clave;</li>
 *   <li>el prestador debe tener un operador asignado; si no, se rechaza y se registra la incidencia (RF-32);</li>
 *   <li>la solicitud valida la fecha, los campos de su tipo y las personas, y ReglaNoDuplicado revisa las abiertas
 *   del turista (RF-31);</li>
 *   <li>se guarda Pendiente con sus notificaciones al turista y al operador (RF-34, RF-44).</li>
 * </ol>
 */
public class EnviarSolicitudService implements EnviarSolicitudUseCase {

    private final SolicitudRepositoryPort solicitudes;
    private final CatalogoConsultaPort catalogo;
    private final PrestadoresPort prestadores;
    private final ContactoPort contactos;
    private final NotificacionRepositoryPort notificaciones;
    private final EjecucionIdempotente idempotente;
    private final Clock reloj;

    public EnviarSolicitudService(SolicitudRepositoryPort solicitudes, CatalogoConsultaPort catalogo,
                                  PrestadoresPort prestadores, ContactoPort contactos,
                                  NotificacionRepositoryPort notificaciones, EjecucionIdempotente idempotente,
                                  Clock reloj) {
        this.solicitudes = solicitudes;
        this.catalogo = catalogo;
        this.prestadores = prestadores;
        this.contactos = contactos;
        this.notificaciones = notificaciones;
        this.idempotente = idempotente;
        this.reloj = reloj;
    }

    @Override
    public ResultadoIdempotente enviar(EnviarSolicitudCommand comando, PeticionIdempotente peticion,
                                       Function<SolicitudDisponibilidad, RespuestaGuardada> respuesta) {
        return idempotente.ejecutar(peticion, () -> respuesta.apply(crear(comando)));
    }

    private SolicitudDisponibilidad crear(EnviarSolicitudCommand comando) {
        if (contactos.turista(comando.turistaId()).isEmpty()) {
            throw new WhatsappNoVerificadoException();
        }
        OfertaSolicitable oferta = catalogo.oferta(comando.ofertaId())
                .orElseThrow(() -> new OfertaNoEncontradaException(comando.ofertaId()));
        UUID operadorId = prestadores.operadorAsignado(oferta.datos().prestadorId())
                .orElseThrow(() -> new PrestadorSinOperadorException(oferta.datos().prestadorId(), oferta.id()));
        CamposDelTipo campos = new CamposDelTipo(comando.horaSalida(), comando.horaReserva(), comando.fechaSalida(),
                comando.numeroUnidades());
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), comando.turistaId(),
                oferta, comando.fechaInicio(), campos, comando.personas(), comando.nota(), operadorId,
                reloj.instant());
        ReglaNoDuplicado.verificar(comando.turistaId(), oferta.id(), comando.fechaInicio(), campos.hora(),
                solicitudes.abiertasDelTurista(comando.turistaId(), oferta.id(), comando.fechaInicio()));
        solicitudes.insertar(solicitud);
        notificaciones.registrar(solicitud.extraerNotificaciones());
        return solicitud;
    }
}
