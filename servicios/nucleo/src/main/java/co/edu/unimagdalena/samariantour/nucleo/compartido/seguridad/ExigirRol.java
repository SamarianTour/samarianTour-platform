package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Autorización por prefijo de ruta (DN-05): antes de llegar al controlador, verifica que la petición traiga una
 * identidad y que su rol sea uno de los que la ruta admite. Deja la identidad en la petición para {@link Identidad#de}.
 *
 * <p>Sin identidad responde 401 {@code NO_AUTENTICADO}; con otro rol, 403 {@code ACCESO_DENEGADO}.
 */
public class ExigirRol implements HandlerInterceptor {

    private final ResolutorIdentidad resolutor;
    private final Set<Rol> admitidos;

    public ExigirRol(ResolutorIdentidad resolutor, Rol requerido) {
        this(resolutor, EnumSet.of(requerido));
    }

    /** Para las rutas que admiten varios roles, como {@code /cuenta/**}, que es de cualquier usuario (RF-12). */
    public ExigirRol(ResolutorIdentidad resolutor, Set<Rol> admitidos) {
        this.resolutor = Objects.requireNonNull(resolutor, "resolutor");
        this.admitidos = EnumSet.copyOf(admitidos);
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object handler) {
        Identidad identidad = resolutor.identidadDe(peticion);
        if (!admitidos.contains(identidad.rol())) {
            throw new AccesoDenegadoException(admitidos.size() == 1
                    ? "Esta ruta es solo para el rol " + admitidos.iterator().next()
                    : "Esta ruta es solo para los roles " + admitidos);
        }
        peticion.setAttribute(Identidad.ATRIBUTO, identidad);
        return true;
    }
}
