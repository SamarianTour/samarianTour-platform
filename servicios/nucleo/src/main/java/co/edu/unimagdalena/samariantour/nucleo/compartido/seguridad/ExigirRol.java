package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

/**
 * Autorización por prefijo de ruta (DN-05): antes de llegar al controlador, verifica que la petición traiga una
 * identidad y que su rol sea el que la ruta exige. Deja la identidad en la petición para {@link Identidad#de}.
 *
 * <p>Sin identidad responde 401 {@code NO_AUTENTICADO}; con otro rol, 403 {@code ACCESO_DENEGADO}.
 */
public class ExigirRol implements HandlerInterceptor {

    private final ResolutorIdentidad resolutor;
    private final Rol requerido;

    public ExigirRol(ResolutorIdentidad resolutor, Rol requerido) {
        this.resolutor = Objects.requireNonNull(resolutor, "resolutor");
        this.requerido = Objects.requireNonNull(requerido, "requerido");
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object handler) {
        Identidad identidad = resolutor.identidadDe(peticion);
        if (identidad.rol() != requerido) {
            throw new AccesoDenegadoException("Esta ruta es solo para el rol " + requerido);
        }
        peticion.setAttribute(Identidad.ATRIBUTO, identidad);
        return true;
    }
}
