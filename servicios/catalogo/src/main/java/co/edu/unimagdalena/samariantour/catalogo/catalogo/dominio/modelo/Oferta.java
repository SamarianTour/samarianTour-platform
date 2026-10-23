package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEditableException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglaTipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglaVisibilidad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Identidad estable de un servicio publicado por un prestador; solicitudes y opiniones apuntan aquí.
 * Su contenido vive en sus versiones: como máximo una publicada y una pendiente, y al menos una de las dos (RF-66).
 */
public final class Oferta {

    /** Revisión de una oferta que todavía no se ha guardado. */
    public static final long REVISION_NUEVA = 0;

    private final String id;
    private final TipoOferta tipo;
    private final UUID prestadorId;
    private boolean retirada;
    private boolean rntPrestadorActivo;
    private long versionHabilitacion;
    private final long revision;
    private VersionOferta publicada;
    private VersionOferta pendiente;

    private Oferta(String id, TipoOferta tipo, UUID prestadorId, boolean retirada, boolean rntPrestadorActivo,
                   long versionHabilitacion, long revision, VersionOferta publicada, VersionOferta pendiente) {
        this.id = Validaciones.texto(id, "id");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.prestadorId = Objects.requireNonNull(prestadorId, "prestadorId");
        if (publicada == null && pendiente == null) {
            throw new IllegalArgumentException("Una oferta tiene al menos una versión publicada o pendiente");
        }
        this.retirada = retirada;
        this.rntPrestadorActivo = rntPrestadorActivo;
        this.versionHabilitacion = versionHabilitacion;
        this.revision = revision;
        this.publicada = publicada;
        this.pendiente = pendiente;
    }

    /**
     * Publica una oferta nueva: nace con su primera versión en moderación y sin versión publicada.
     *
     * @param tipoPrestador tipo del prestador, tomado del token de acceso
     * @param zona          zona indicada en el contenido; debe estar activa
     */
    public static Oferta publicar(String id, PrestadorCatalogo prestador, TipoPrestador tipoPrestador,
                                  VersionOferta contenido, Zona zona, Instant ahora) {
        return publicar(id, prestador, tipoPrestador, contenido, zona, List.of(), ahora);
    }

    /**
     * Publica una oferta nueva cuyo contenido puede ser un paquete.
     *
     * @param actividades ofertas que referencia el contenido cuando es un paquete; deben ser actividades del mismo
     *                    prestador (RF-22). Para los demás tipos de oferta no se usa
     */
    public static Oferta publicar(String id, PrestadorCatalogo prestador, TipoPrestador tipoPrestador,
                                  VersionOferta contenido, Zona zona, Collection<Oferta> actividades, Instant ahora) {
        verificarPuedePublicar(prestador, tipoPrestador, contenido, zona, actividades);
        return new Oferta(id, contenido.tipo(), prestador.prestadorId(), false, prestador.rntActivo(),
                prestador.version(), REVISION_NUEVA, null, contenido.enviada(1, ahora));
    }

    /**
     * Reconstruye una oferta guardada; solo para los adaptadores de persistencia.
     *
     * @param versionHabilitacion versión del aviso de habilitación copiado junto con {@code rntPrestadorActivo}
     * @param revision            revisión técnica con la que se leyó, para el bloqueo optimista (ADR-05)
     */
    public static Oferta reconstituir(String id, TipoOferta tipo, UUID prestadorId, boolean retirada,
                                      boolean rntPrestadorActivo, long versionHabilitacion, long revision,
                                      VersionOferta publicada, VersionOferta pendiente) {
        return new Oferta(id, tipo, prestadorId, retirada, rntPrestadorActivo, versionHabilitacion, revision,
                publicada, pendiente);
    }

    /**
     * Crea una versión pendiente con número nuevo. Reemplaza a la pendiente anterior, esté en moderación o rechazada.
     * La versión publicada sigue visible hasta que se apruebe la nueva (RF-66).
     */
    public void editar(PrestadorCatalogo prestador, TipoPrestador tipoPrestador, VersionOferta contenido, Zona zona,
                       Instant ahora) {
        editar(prestador, tipoPrestador, contenido, zona, List.of(), ahora);
    }

    /**
     * Edita una oferta cuyo contenido puede ser un paquete.
     *
     * @param actividades ofertas que referencia el contenido cuando es un paquete; deben ser actividades del mismo
     *                    prestador (RF-22). Para los demás tipos de oferta no se usa
     */
    public void editar(PrestadorCatalogo prestador, TipoPrestador tipoPrestador, VersionOferta contenido, Zona zona,
                       Collection<Oferta> actividades, Instant ahora) {
        verificarDueno(prestador.prestadorId());
        if (retirada) {
            throw new OfertaNoEditableException(id);
        }
        if (contenido.tipo() != tipo) {
            throw new DatoInvalidoException("tipo", "el tipo de una oferta no cambia después de creada");
        }
        verificarPuedePublicar(prestador, tipoPrestador, contenido, zona, actividades);
        pendiente = contenido.enviada(siguienteNumeroVersion(), ahora);
        copiarHabilitacion(prestador);
    }

    /** Retira la oferta del catálogo. Es definitivo (RF-65) y se permite aunque el RNT esté inactivo. */
    public void retirar(UUID solicitante) {
        verificarDueno(solicitante);
        retirada = true;
    }

    /**
     * Aplica la aprobación de la versión indicada. Es idempotente: si la versión ya no está en moderación
     * (ya se aplicó, fue reemplazada o la oferta está retirada) no cambia nada.
     *
     * @param habilitacion réplica de la habilitación del dueño; se vuelve a copiar porque solo a partir de aquí la
     *                     oferta puede ser visible (DC-03, ADR-05)
     * @return true si la aprobación cambió la oferta
     */
    public boolean aprobar(int numeroVersion, PrestadorCatalogo habilitacion, Instant ahora) {
        if (!esDe(habilitacion.prestadorId())) {
            throw new IllegalArgumentException("La habilitación no es la del dueño de la oferta");
        }
        if (!tienePendienteEnModeracion(numeroVersion)) {
            return false;
        }
        pendiente.aprobar(ahora);
        publicada = pendiente;
        pendiente = null;
        copiarHabilitacion(habilitacion);
        return true;
    }

    /**
     * Aplica el rechazo de la versión indicada; la versión rechazada queda como pendiente con su motivo.
     * Es idempotente, igual que {@link #aprobar}.
     *
     * @return true si el rechazo cambió la oferta
     */
    public boolean rechazar(int numeroVersion, String motivo, Instant ahora) {
        Validaciones.texto(motivo, "motivoRechazo");
        if (!tienePendienteEnModeracion(numeroVersion)) {
            return false;
        }
        pendiente.rechazar(motivo, ahora);
        return true;
    }

    /**
     * Visibilidad según ReglaVisibilidad. Para un paquete es necesaria pero no suficiente: además todas sus
     * actividades deben ser visibles, y eso lo decide {@link ReglasPaquete#esVisible} con ellas cargadas (RF-22).
     */
    public boolean estaVisible() {
        return ReglaVisibilidad.esVisible(this);
    }

    /**
     * Si espera la decisión del administrador (RF-79): no está retirada y su versión pendiente está en moderación.
     * Una versión rechazada no espera nada: espera a que el prestador edite (DC-06).
     */
    public boolean estaEnModeracion() {
        return !retirada && pendiente != null && pendiente.estadoModeracion() == EstadoModeracion.EN_MODERACION;
    }

    public boolean esDe(UUID prestador) {
        return prestadorId.equals(prestador);
    }

    private static void verificarPuedePublicar(PrestadorCatalogo prestador, TipoPrestador tipoPrestador,
                                               VersionOferta contenido, Zona zona, Collection<Oferta> actividades) {
        if (!prestador.rntActivo()) {
            throw new RntInactivoException(prestador.prestadorId());
        }
        ReglaTipoOferta.verificar(Objects.requireNonNull(tipoPrestador, "tipoPrestador"), contenido);
        if (contenido instanceof VersionPaquete paquete) {
            ReglasPaquete.verificarActividades(prestador.prestadorId(), paquete, actividades);
        }
        if (!zona.id().equals(contenido.contenido().zonaId())) {
            throw new IllegalArgumentException("La zona recibida no es la del contenido");
        }
        if (!zona.activa()) {
            throw new ZonaNoDisponibleException(zona.id());
        }
    }

    /** La copia del RNT y la versión del aviso que la produjo siempre se copian juntas (ADR-05). */
    private void copiarHabilitacion(PrestadorCatalogo prestador) {
        rntPrestadorActivo = prestador.rntActivo();
        versionHabilitacion = prestador.version();
    }

    private void verificarDueno(UUID solicitante) {
        if (!esDe(solicitante)) {
            throw new PrestadorNoAutorizadoException(id);
        }
    }

    private boolean tienePendienteEnModeracion(int numeroVersion) {
        return estaEnModeracion() && pendiente.numero() == numeroVersion;
    }

    /** El número más alto emitido siempre sigue vivo (publicada o pendiente), así que nunca se reutiliza. */
    private int siguienteNumeroVersion() {
        int publicadaNumero = publicada == null ? 0 : publicada.numero();
        int pendienteNumero = pendiente == null ? 0 : pendiente.numero();
        return Math.max(publicadaNumero, pendienteNumero) + 1;
    }

    public String id() {
        return id;
    }

    public TipoOferta tipo() {
        return tipo;
    }

    public UUID prestadorId() {
        return prestadorId;
    }

    public boolean retirada() {
        return retirada;
    }

    /** Copia de la habilitación RNT del prestador, para que la búsqueda use solo el índice de ofertas (QAS-07). */
    public boolean rntPrestadorActivo() {
        return rntPrestadorActivo;
    }

    /** Versión del aviso de habilitación del que viene la copia del RNT; solo avanza (ADR-05). */
    public long versionHabilitacion() {
        return versionHabilitacion;
    }

    /**
     * Revisión técnica para el bloqueo optimista (ADR-05). Ninguna regla de negocio la interpreta: la asigna
     * la persistencia y no es {@link VersionOferta#numero()}.
     */
    public long revision() {
        return revision;
    }

    public Optional<VersionOferta> publicada() {
        return Optional.ofNullable(publicada);
    }

    public Optional<VersionOferta> pendiente() {
        return Optional.ofNullable(pendiente);
    }
}
