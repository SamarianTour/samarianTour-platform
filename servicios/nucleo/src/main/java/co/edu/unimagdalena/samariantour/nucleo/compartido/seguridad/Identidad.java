package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

import co.edu.unimagdalena.samariantour.seguridadjwt.AutenticacionToken;
import co.edu.unimagdalena.samariantour.seguridadjwt.ReclamosToken;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuario que hace la petición, tal como lo dice su token de acceso (DN-37). El tipo de prestador llega como texto
 * porque el paquete compartido no conoce los tipos de los módulos: cada módulo lo convierte al suyo.
 *
 * @param tipoPrestador solo del prestador que ya registró su RNT; null en los demás casos
 */
public record Identidad(UUID usuarioId, Rol rol, String tipoPrestador) {

    public Identidad {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(rol, "rol");
        if (tipoPrestador != null && rol != Rol.PRESTADOR) {
            throw new IllegalArgumentException("Solo el prestador tiene tipo");
        }
    }

    /**
     * La identidad del token que Spring Security ya validó para esta petición. Las rutas de cada rol siempre la
     * tienen ({@code SeguridadConfig}).
     *
     * @throws NoAutenticadoException si la petición no trae un token válido
     */
    public static Identidad de(HttpServletRequest peticion) {
        if (peticion.getUserPrincipal() instanceof AutenticacionToken autenticacion) {
            ReclamosToken reclamos = autenticacion.getPrincipal();
            return new Identidad(reclamos.usuarioId(), rol(reclamos.rol()), reclamos.tipoPrestador());
        }
        throw new NoAutenticadoException("Esta ruta no recibe la identidad del usuario");
    }

    private static Rol rol(String nombre) {
        return Arrays.stream(Rol.values())
                .filter(rol -> rol.name().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new AccesoDenegadoException("El rol " + nombre + " no existe en el núcleo"));
    }
}
