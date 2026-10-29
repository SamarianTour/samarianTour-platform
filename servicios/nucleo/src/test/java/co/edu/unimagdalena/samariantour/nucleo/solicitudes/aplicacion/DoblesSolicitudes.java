package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudDuplicadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ContactoTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionPendiente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OfertaSolicitable;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OperacionRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ContactoPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.IdempotenciaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;

/**
 * Dobles en memoria de los puertos de salida de las solicitudes, para las pruebas de la aplicación (DN-03). El
 * repositorio guarda copias con su revisión, como PostgreSQL: una copia vieja que se guarda choca.
 */
final class DoblesSolicitudes {

    private DoblesSolicitudes() {
    }

    static EjecutorConReintento sinEspera() {
        return new EjecutorConReintento(3, Duration.ZERO);
    }

    static Clock reloj(Instant ahora) {
        return Clock.fixed(ahora, ZoneOffset.UTC);
    }

    /** Las solicitudes, guardadas como copias con su revisión. */
    static class Repositorio implements SolicitudRepositoryPort {

        private final Map<UUID, SolicitudDisponibilidad> guardadas = new LinkedHashMap<>();
        private final Map<UUID, Long> revisiones = new HashMap<>();
        int actualizaciones;

        @Override
        public void insertar(SolicitudDisponibilidad solicitud) {
            boolean duplicada = guardadas.values().stream().anyMatch(otra -> !otra.estado().esTerminal()
                    && otra.esDelTurista(solicitud.turistaId()) && otra.ofertaId().equals(solicitud.ofertaId())
                    && otra.fechaInicio().equals(solicitud.fechaInicio())
                    && Objects.equals(otra.campos().hora(), solicitud.campos().hora()));
            if (duplicada) {
                throw new SolicitudDuplicadaException();
            }
            revisiones.put(solicitud.id(), 0L);
            guardadas.put(solicitud.id(), copia(solicitud, 0L));
        }

        @Override
        public void actualizar(SolicitudDisponibilidad solicitud) {
            long actual = revisiones.get(solicitud.id());
            if (solicitud.revision() == null || solicitud.revision() != actual) {
                throw new ConflictoConcurrenciaException("La solicitud cambió desde que se leyó");
            }
            actualizaciones++;
            revisiones.put(solicitud.id(), actual + 1);
            guardadas.put(solicitud.id(), copia(solicitud, actual + 1));
        }

        @Override
        public Optional<SolicitudDisponibilidad> buscar(UUID id) {
            return Optional.ofNullable(guardadas.get(id)).map(s -> copia(s, revisiones.get(id)));
        }

        @Override
        public List<SolicitudDisponibilidad> abiertasDelTurista(UUID turistaId, String ofertaId, LocalDate fecha) {
            return guardadas.values().stream()
                    .filter(s -> s.esDelTurista(turistaId) && s.ofertaId().equals(ofertaId)
                            && s.fechaInicio().equals(fecha) && !s.estado().esTerminal())
                    .map(s -> copia(s, revisiones.get(s.id())))
                    .toList();
        }

        @Override
        public Pagina<SolicitudDisponibilidad> delTurista(UUID turistaId, Paginacion paginacion) {
            return paginar(guardadas.values().stream().filter(s -> s.esDelTurista(turistaId))
                    .sorted(Comparator.comparing(SolicitudDisponibilidad::creadaEn).reversed()).toList(), paginacion);
        }

        @Override
        public Pagina<SolicitudDisponibilidad> delOperador(UUID operadorId, EstadoSolicitud estado,
                                                           Paginacion paginacion) {
            Comparator<SolicitudDisponibilidad> orden = Comparator.comparing(SolicitudDisponibilidad::creadaEn);
            return paginar(guardadas.values().stream()
                    .filter(s -> s.esDelOperador(operadorId) && s.estado() == estado)
                    .sorted(estado == EstadoSolicitud.PENDIENTE ? orden : orden.reversed()).toList(), paginacion);
        }

        @Override
        public List<SolicitudDisponibilidad> tomarPorVencer(Instant ahora, int limite) {
            return guardadas.values().stream()
                    .filter(s -> s.limiteVencimiento() != null && !s.limiteVencimiento().isAfter(ahora))
                    .sorted(Comparator.comparing(SolicitudDisponibilidad::limiteVencimiento))
                    .limit(limite)
                    .map(s -> copia(s, revisiones.get(s.id())))
                    .toList();
        }

        @Override
        public List<UUID> prestadoresConAbiertas() {
            return guardadas.values().stream()
                    .filter(s -> !s.estado().esTerminal())
                    .map(s -> s.oferta().prestadorId())
                    .distinct()
                    .toList();
        }

        @Override
        public List<SolicitudDisponibilidad> tomarAbiertasDelPrestador(UUID prestadorId, int limite) {
            return guardadas.values().stream()
                    .filter(s -> !s.estado().esTerminal() && !s.intentoPagoEnCurso()
                            && s.oferta().prestadorId().equals(prestadorId))
                    .limit(limite)
                    .map(s -> copia(s, revisiones.get(s.id())))
                    .toList();
        }

        int cantidad() {
            return guardadas.size();
        }

        private Pagina<SolicitudDisponibilidad> paginar(List<SolicitudDisponibilidad> todas, Paginacion paginacion) {
            List<SolicitudDisponibilidad> pagina = todas.stream()
                    .skip((long) paginacion.pagina() * paginacion.tamano()).limit(paginacion.tamano())
                    .map(s -> copia(s, revisiones.get(s.id()))).toList();
            return new Pagina<>(pagina, paginacion.pagina(), paginacion.tamano(), todas.size());
        }

        private static SolicitudDisponibilidad copia(SolicitudDisponibilidad s, long revision) {
            return SolicitudDisponibilidad.reconstituir(s.id(), s.turistaId(), s.ofertaId(), s.oferta(),
                    s.fechaInicio(), s.campos(), s.personas(), s.notaTurista(), s.operadorId(), s.creadaEn(),
                    s.estado(), s.condiciones(), s.aparteHasta(), s.venceEn(), s.intentoPagoEnCurso(),
                    s.intentosPagoFallidos(), s.actualizadaEn(), revision, s.historial());
        }
    }

    /** Las notificaciones registradas, en orden. */
    static final class Notificaciones implements NotificacionRepositoryPort {

        final List<NotificacionSolicitud> registradas = new ArrayList<>();

        @Override
        public void registrar(List<NotificacionSolicitud> notificaciones) {
            registradas.addAll(notificaciones);
        }

        @Override
        public List<NotificacionPendiente> tomarPendientes(Instant ahora, int limite) {
            return List.of();
        }

        @Override
        public void guardar(NotificacionPendiente notificacion) {
        }
    }

    /** Las claves de idempotencia de una sola transacción a la vez. */
    static final class Claves implements IdempotenciaPort {

        private final Map<String, OperacionRegistrada> registradas = new HashMap<>();
        private final Map<String, PeticionIdempotente> reservadas = new HashMap<>();

        @Override
        public Optional<OperacionRegistrada> reservar(PeticionIdempotente peticion, Instant ahora) {
            String llave = peticion.usuarioId() + "/" + peticion.clave();
            if (registradas.containsKey(llave)) {
                return Optional.of(registradas.get(llave));
            }
            reservadas.put(llave, peticion);
            return Optional.empty();
        }

        @Override
        public void completar(PeticionIdempotente peticion, RespuestaGuardada respuesta) {
            String llave = peticion.usuarioId() + "/" + peticion.clave();
            reservadas.remove(llave);
            registradas.put(llave, new OperacionRegistrada(peticion.operacion(), peticion.huella(), respuesta));
        }

        @Override
        public int purgarVencidas(Instant ahora) {
            return 0;
        }
    }

    /** Corre la operación tal cual: las pruebas de la aplicación no necesitan una base. */
    static final class TransaccionDirecta implements TransaccionPort {

        int abiertas;

        @Override
        public <T> T ejecutar(Supplier<T> operacion) {
            abiertas++;
            return operacion.get();
        }
    }

    /** Un catálogo con ofertas fijas; caído, lanza lo que se le indique. */
    static final class Catalogo implements CatalogoConsultaPort {

        private final Map<String, OfertaSolicitable> ofertas = new HashMap<>();
        RuntimeException falla;
        int consultas;

        Catalogo con(OfertaSolicitable oferta) {
            ofertas.put(oferta.id(), oferta);
            return this;
        }

        @Override
        public Optional<OfertaSolicitable> oferta(String ofertaId) {
            consultas++;
            if (falla != null) {
                throw falla;
            }
            return Optional.ofNullable(ofertas.get(ofertaId));
        }
    }

    /** La agencia con el operador 1 y la turista de la demo con su WhatsApp. */
    static final class Directorio implements PrestadoresPort, ContactoPort {

        final Map<UUID, UUID> operadores = new HashMap<>(Map.of(AGENCIA, OPERADOR));
        /** Los prestadores sin el RNT activo; los demás están habilitados. */
        final Set<UUID> inhabilitados = new HashSet<>();
        final Map<UUID, ContactoTurista> turistas = new HashMap<>(
                Map.of(TURISTA, new ContactoTurista("Laura Gómez", "+573105550001")));

        @Override
        public Optional<UUID> operadorAsignado(UUID prestadorId) {
            return Optional.ofNullable(operadores.get(prestadorId));
        }

        @Override
        public Optional<String> whatsapp(UUID prestadorId) {
            return Optional.of("+573001110001");
        }

        @Override
        public boolean estaHabilitado(UUID prestadorId) {
            return !inhabilitados.contains(prestadorId);
        }

        @Override
        public Optional<ContactoTurista> turista(UUID turistaId) {
            return Optional.ofNullable(turistas.get(turistaId));
        }
    }
}
