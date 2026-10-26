package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.AvisoPlataforma;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CanalNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Destinatario;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoEnvio;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionPendiente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.AvisoPlataformaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Las notificaciones en {@code notificaciones_solicitud} (DN-13), y los avisos del operador, que son las mismas filas
 * con el canal PLATAFORMA (RF-34).
 */
@Component
public class NotificacionJpaAdapter implements NotificacionRepositoryPort, AvisoPlataformaPort {

    private static final TypeReference<Map<String, String>> DATOS = new TypeReference<>() { };

    private final NotificacionSolicitudJpaRepository repositorio;
    private final ObjectMapper json;

    NotificacionJpaAdapter(NotificacionSolicitudJpaRepository repositorio, ObjectMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    @Override
    public void registrar(List<NotificacionSolicitud> notificaciones) {
        repositorio.saveAll(notificaciones.stream().map(this::aFila).toList());
    }

    @Override
    public List<NotificacionPendiente> tomarPendientes(Instant ahora, int limite) {
        return repositorio.tomarPendientes(ahora, limite).stream()
                .map(fila -> new NotificacionPendiente(fila.id(), fila.solicitudId(), destinatario(fila),
                        TipoNotificacion.valueOf(fila.tipo()), datos(fila), fila.intentos()))
                .toList();
    }

    @Override
    public void guardar(NotificacionPendiente notificacion) {
        NotificacionSolicitudEntity fila = repositorio.findById(notificacion.id()).orElseThrow();
        fila.registrarIntento(notificacion.estado().name(), notificacion.intentos(), notificacion.proximoIntento(),
                notificacion.enviadaEn(), notificacion.ultimoError());
    }

    @Override
    public Pagina<AvisoPlataforma> avisosDe(UUID operadorId, Paginacion paginacion) {
        Page<NotificacionSolicitudEntity> filas = repositorio
                .findByCanalAndDestinatarioTipoAndDestinatarioIdOrderByCreadaEnDescIdAsc(
                        CanalNotificacion.PLATAFORMA.name(), Destinatario.Tipo.OPERADOR.name(), operadorId,
                        PageRequest.of(paginacion.pagina(), paginacion.tamano()));
        return new Pagina<>(filas.getContent().stream()
                .map(fila -> new AvisoPlataforma(fila.id(), fila.solicitudId(), TipoNotificacion.valueOf(fila.tipo()),
                        datos(fila), fila.creadaEn()))
                .toList(), paginacion.pagina(), paginacion.tamano(), filas.getTotalElements());
    }

    /** Las de la plataforma ya quedan entregadas: el operador las ve en cuanto se confirma el cambio. */
    private NotificacionSolicitudEntity aFila(NotificacionSolicitud notificacion) {
        boolean plataforma = notificacion.canal() == CanalNotificacion.PLATAFORMA;
        return new NotificacionSolicitudEntity(notificacion.id(), notificacion.solicitudId(),
                notificacion.destinatario().tipo().name(), notificacion.destinatario().id(),
                notificacion.canal().name(), notificacion.tipo().name(), aJson(notificacion.datos()),
                (plataforma ? EstadoEnvio.ENVIADA : EstadoEnvio.PENDIENTE).name(), notificacion.creadaEn(),
                notificacion.creadaEn(), plataforma ? notificacion.creadaEn() : null);
    }

    private static Destinatario destinatario(NotificacionSolicitudEntity fila) {
        return new Destinatario(Destinatario.Tipo.valueOf(fila.destinatarioTipo()), fila.destinatarioId());
    }

    private String aJson(Map<String, String> datos) {
        try {
            return json.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Los datos de la notificación no se pueden convertir a JSON", e);
        }
    }

    private Map<String, String> datos(NotificacionSolicitudEntity fila) {
        try {
            return json.readValue(fila.datos(), DATOS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Los datos de la notificación " + fila.id() + " no son JSON", e);
        }
    }
}
