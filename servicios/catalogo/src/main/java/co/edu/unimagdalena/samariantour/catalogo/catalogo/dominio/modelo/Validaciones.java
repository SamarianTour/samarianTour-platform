package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Validaciones de campo del dominio, reunidas en un solo lugar.
 * Pendiente: completar longitudes y rangos cuando se cierre el SRS (DC-19 de docs/decisiones-catalogo-ofertas.md).
 */
public final class Validaciones {

    private Validaciones() {
    }

    /** Texto obligatorio; se devuelve sin espacios al inicio ni al final. */
    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException(campo, "es obligatorio");
        }
        return valor.strip();
    }

    /** Texto opcional; un texto en blanco se trata como ausente. */
    public static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    public static <T> T presente(T valor, String campo) {
        if (valor == null) {
            throw new DatoInvalidoException(campo, "es obligatorio");
        }
        return valor;
    }

    public static int positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new DatoInvalidoException(campo, "debe ser mayor que cero");
        }
        return valor;
    }

    public static Integer positivoOpcional(Integer valor, String campo) {
        return valor == null ? null : positivo(valor, campo);
    }

    public static BigDecimal noNegativo(BigDecimal valor, String campo) {
        if (presente(valor, campo).signum() < 0) {
            throw new DatoInvalidoException(campo, "no puede ser negativo");
        }
        return valor;
    }

    /** Lista inmutable; null se trata como lista vacía y no admite elementos nulos. */
    public static <T> List<T> lista(List<T> valores, String campo) {
        if (valores == null) {
            return List.of();
        }
        if (valores.stream().anyMatch(Objects::isNull)) {
            throw new DatoInvalidoException(campo, "no admite elementos vacíos");
        }
        return List.copyOf(valores);
    }

    public static <T> List<T> listaNoVacia(List<T> valores, String campo) {
        List<T> copia = lista(valores, campo);
        if (copia.isEmpty()) {
            throw new DatoInvalidoException(campo, "debe tener al menos un elemento");
        }
        return copia;
    }
}
