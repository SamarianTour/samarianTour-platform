package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Las solicitudes y su historial en PostgreSQL (DN-02, DN-11). */
public interface SolicitudRepositoryPort {

    /**
     * Guarda una solicitud nueva con su historial.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudDuplicadaException si el
     *         índice de no duplicado la rechaza: otro envío igual se guardó un instante antes (RF-31)
     */
    void insertar(SolicitudDisponibilidad solicitud);

    /**
     * Guarda los cambios de una solicitud leída y agrega sus transiciones nuevas, solo si nadie la cambió desde que se
     * leyó (bloqueo optimista, ADR-05).
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException si otra
     *         escritura la cambió en medio; no guarda nada
     */
    void actualizar(SolicitudDisponibilidad solicitud);

    Optional<SolicitudDisponibilidad> buscar(UUID id);

    /** Las no terminales del turista para esa oferta y fecha: las que ReglaNoDuplicado revisa. */
    List<SolicitudDisponibilidad> abiertasDelTurista(UUID turistaId, String ofertaId, LocalDate fechaInicio);

    /** Las del turista, de la más reciente a la más antigua (RF-33). */
    Pagina<SolicitudDisponibilidad> delTurista(UUID turistaId, Paginacion paginacion);

    /**
     * Las del operador en ese estado: las pendientes de la más antigua a la más reciente, su cola (RF-35); las demás,
     * de la más reciente a la más antigua, su historial de trámites.
     */
    Pagina<SolicitudDisponibilidad> delOperador(UUID operadorId, EstadoSolicitud estado, Paginacion paginacion);

    /**
     * Toma para esta transacción hasta {@code limite} solicitudes abiertas cuyo vencimiento ya toca, de la que vence
     * antes a la que vence después, bloqueadas hasta el final de la transacción. Las que otra transacción tiene
     * bloqueadas se saltan ({@code FOR UPDATE SKIP LOCKED}): la otra réplica las está venciendo, o el operador las está
     * atendiendo.
     */
    List<SolicitudDisponibilidad> tomarPorVencer(Instant ahora, int limite);

    /** Los prestadores con alguna solicitud abierta (RF-77). */
    List<UUID> prestadoresConAbiertas();

    /**
     * Toma para esta transacción hasta {@code limite} solicitudes abiertas del prestador sin un intento de pago en
     * curso, bloqueadas hasta el final de la transacción; las que otra transacción tiene bloqueadas se saltan
     * ({@code FOR UPDATE SKIP LOCKED}).
     */
    List<SolicitudDisponibilidad> tomarAbiertasDelPrestador(UUID prestadorId, int limite);
}
