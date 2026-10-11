package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.time.Instant;
import java.util.Objects;

/**
 * Datos de moderación de una versión de oferta: su número dentro de la oferta, su estado y las marcas de tiempo.
 * Es inmutable: cada decisión de moderación produce un registro nuevo.
 *
 * @param numero crece siempre dentro de la oferta, también cuando una edición reemplaza a la pendiente;
 *               el aviso de moderación lo nombra para no aprobar una versión distinta de la revisada
 */
public record RegistroModeracion(
        int numero,
        EstadoModeracion estado,
        String motivoRechazo,
        Instant enviadaEn,
        Instant revisadaEn) {

    public RegistroModeracion {
        if (numero < 1) {
            throw new IllegalArgumentException("El número de versión empieza en 1");
        }
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(enviadaEn, "enviadaEn");
        boolean revisada = estado != EstadoModeracion.EN_MODERACION;
        if (revisada != (revisadaEn != null)) {
            throw new IllegalArgumentException("revisadaEn existe si y solo si la versión ya fue revisada");
        }
        if ((estado == EstadoModeracion.RECHAZADA) != (motivoRechazo != null)) {
            throw new IllegalArgumentException("motivoRechazo existe si y solo si la versión fue rechazada");
        }
    }

    static RegistroModeracion enviada(int numero, Instant ahora) {
        return new RegistroModeracion(numero, EstadoModeracion.EN_MODERACION, null, ahora, null);
    }

    RegistroModeracion aprobada(Instant ahora) {
        return new RegistroModeracion(numero, EstadoModeracion.APROBADA, null, enviadaEn, ahora);
    }

    RegistroModeracion rechazada(String motivo, Instant ahora) {
        return new RegistroModeracion(numero, EstadoModeracion.RECHAZADA,
                Validaciones.texto(motivo, "motivoRechazo"), enviadaEn, ahora);
    }
}
