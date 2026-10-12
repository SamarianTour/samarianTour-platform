package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEditableException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglaTipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglaVisibilidad;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Identidad estable de un servicio publicado por un prestador; solicitudes y opiniones apuntan aquí.
 * Su contenido vive en sus versiones: como máximo una publicada y una pendiente, y al menos una de las dos (RF-66).
 */
public final class Oferta {

    private final String id;
    private final TipoOferta tipo;
    private final UUID prestadorId;
    private boolean retirada;
    private boolean rntPrestadorActivo;
    private VersionOferta publicada;
    private VersionOferta pendiente;

    private Oferta(String id, TipoOferta tipo, UUID prestadorId, boolean retirada, boolean rntPrestadorActivo,
                   VersionOferta publicada, VersionOferta pendiente) {
        this.id = Validaciones.texto(id, "id");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.prestadorId = Objects.requireNonNull(prestadorId, "prestadorId");
        if (publicada == null && pendiente == null) {
            throw new IllegalArgumentException("Una oferta tiene al menos una versión publicada o pendiente");
        }
        this.retirada = retirada;
        this.rntPrestadorActivo = rntPrestadorActivo;
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
        verificarPuedePublicar(prestador, tipoPrestador, contenido, zona);
        contenido.enviar(1, ahora);
        return new Oferta(id, contenido.tipo(), prestador.prestadorId(), false, prestador.rntActivo(), null, contenido);
    }

    /** Reconstruye una oferta guardada; solo para los adaptadores de persistencia. */
    public static Oferta reconstituir(String id, TipoOferta tipo, UUID prestadorId, boolean retirada,
                                      boolean rntPrestadorActivo, VersionOferta publicada, VersionOferta pendiente) {
        return new Oferta(id, tipo, prestadorId, retirada, rntPrestadorActivo, publicada, pendiente);
    }

    /**
     * Crea una versión pendiente con número nuevo. Reemplaza a la pendiente anterior, esté en moderación o rechazada.
     * La versión publicada sigue visible hasta que se apruebe la nueva (RF-66).
     */
    public void editar(PrestadorCatalogo prestador, TipoPrestador tipoPrestador, VersionOferta contenido, Zona zona,
                       Instant ahora) {
        verificarDueno(prestador.prestadorId());
        if (retirada) {
            throw new OfertaNoEditableException(id);
        }
        if (contenido.tipo() != tipo) {
            throw new DatoInvalidoException("tipo", "el tipo de una oferta no cambia después de creada");
        }
        verificarPuedePublicar(prestador, tipoPrestador, contenido, zona);
        contenido.enviar(siguienteNumeroVersion(), ahora);
        pendiente = contenido;
        rntPrestadorActivo = prestador.rntActivo();
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
     * @param rntActivoActual habilitación vigente según la réplica; se vuelve a copiar porque solo a partir de aquí
     *                        la oferta puede ser visible
     * @return true si la aprobación cambió la oferta
     */
    public boolean aprobar(int numeroVersion, boolean rntActivoActual, Instant ahora) {
        if (!tienePendienteEnModeracion(numeroVersion)) {
            return false;
        }
        pendiente.aprobar(ahora);
        publicada = pendiente;
        pendiente = null;
        rntPrestadorActivo = rntActivoActual;
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

    public boolean estaVisible() {
        return ReglaVisibilidad.esVisible(this);
    }

    public boolean esDe(UUID prestador) {
        return prestadorId.equals(prestador);
    }

    private static void verificarPuedePublicar(PrestadorCatalogo prestador, TipoPrestador tipoPrestador,
                                               VersionOferta contenido, Zona zona) {
        if (!prestador.rntActivo()) {
            throw new RntInactivoException(prestador.prestadorId());
        }
        ReglaTipoOferta.verificar(Objects.requireNonNull(tipoPrestador, "tipoPrestador"), contenido);
        if (!zona.id().equals(contenido.contenido().zonaId())) {
            throw new IllegalArgumentException("La zona recibida no es la del contenido");
        }
        if (!zona.activa()) {
            throw new ZonaNoDisponibleException(zona.id());
        }
    }

    private void verificarDueno(UUID solicitante) {
        if (!esDe(solicitante)) {
            throw new PrestadorNoAutorizadoException(id);
        }
    }

    private boolean tienePendienteEnModeracion(int numeroVersion) {
        return !retirada
                && pendiente != null
                && pendiente.numero() == numeroVersion
                && pendiente.estadoModeracion() == EstadoModeracion.EN_MODERACION;
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

    public Optional<VersionOferta> publicada() {
        return Optional.ofNullable(publicada);
    }

    public Optional<VersionOferta> pendiente() {
        return Optional.ofNullable(pendiente);
    }
}
