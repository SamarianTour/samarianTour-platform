package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de una foto que subió un prestador (DC-47): de quién es, para qué la subió y cuándo se usó por última vez.
 * Sirve para que nadie ponga en su oferta la foto de otro prestador, y para borrar las fotos que nadie usa.
 *
 * @param prestadorId prestador que la subió; solo él puede ponerla en sus ofertas
 * @param ultimoUso   cuándo se subió o se vio en uso por última vez. Solo avanza
 */
public record FotoSubida(Foto foto, UUID prestadorId, DestinoFoto destino, Instant ultimoUso) {

    /**
     * Tiempo que una foto sin uso se conserva: es el plazo del prestador para ponerla en una oferta después de subirla.
     * Después, la limpieza la borra si ninguna oferta ni el perfil la usa.
     */
    public static final Duration PLAZO_SIN_USO = Duration.ofHours(24);

    public FotoSubida {
        Objects.requireNonNull(foto, "foto");
        Objects.requireNonNull(prestadorId, "prestadorId");
        Objects.requireNonNull(destino, "destino");
        Objects.requireNonNull(ultimoUso, "ultimoUso");
    }

    /** Si el prestador puede ponerla en sus ofertas: la subió él, y para sus ofertas. */
    public boolean sirveParaOfertasDe(UUID prestador) {
        return destino == DestinoFoto.OFERTAS && prestadorId.equals(prestador);
    }
}
