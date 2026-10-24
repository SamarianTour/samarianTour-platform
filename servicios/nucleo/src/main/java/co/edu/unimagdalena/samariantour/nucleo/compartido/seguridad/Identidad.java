package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;
import java.util.UUID;

/**
 * Usuario que hace la petición: los mismos datos que traerá el JWT del núcleo. El tipo de prestador llega como texto
 * porque el paquete compartido no conoce los tipos de los módulos: cada módulo lo convierte al suyo.
 *
 * @param tipoPrestador solo para el rol {@link Rol#PRESTADOR}; null en los demás roles
 */
public record Identidad(UUID usuarioId, Rol rol, String tipoPrestador) {

    /** Atributo de la petición donde {@link ExigirRol} deja la identidad ya verificada. */
    static final String ATRIBUTO = Identidad.class.getName();

    public Identidad {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(rol, "rol");
        if ((rol == Rol.PRESTADOR) != (tipoPrestador != null)) {
            throw new IllegalArgumentException("El tipo de prestador existe si y solo si el rol es PRESTADOR");
        }
    }

    /**
     * La identidad que {@link ExigirRol} verificó para esta petición. Solo existe en las rutas que exigen un rol
     * ({@code /prestador/**}, {@code /admin/**} y {@code /turista/**}).
     *
     * @throws NoAutenticadoException si la ruta no exige identidad
     */
    public static Identidad de(HttpServletRequest peticion) {
        if (peticion.getAttribute(ATRIBUTO) instanceof Identidad identidad) {
            return identidad;
        }
        throw new NoAutenticadoException("Esta ruta no recibe la identidad del usuario");
    }
}
