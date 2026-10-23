package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.ExigirRol;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.NoAutenticadoException;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.ResolutorIdentidad;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.Rol;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autorización por prefijo de ruta, en un solo lugar para todos los módulos (DC-13): cada prefijo exige un rol.
 * Las demás rutas (búsqueda, ficha, zonas) son públicas, y {@code /internal/**} solo es accesible desde la red
 * interna (Nginx la bloquea hacia afuera).
 */
@Configuration
public class SeguridadConfig implements WebMvcConfigurer {

    private final ResolutorIdentidad resolutor;

    /** Sin un resolutor registrado (algunas pruebas de un solo controlador), toda ruta protegida responde 401. */
    public SeguridadConfig(ObjectProvider<ResolutorIdentidad> resolutor) {
        this.resolutor = resolutor.getIfAvailable(() -> peticion -> {
            throw new NoAutenticadoException("No hay forma de identificar al usuario");
        });
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new ExigirRol(resolutor, Rol.PRESTADOR)).addPathPatterns("/prestador/**");
        registro.addInterceptor(new ExigirRol(resolutor, Rol.ADMINISTRADOR)).addPathPatterns("/admin/**");
        registro.addInterceptor(new ExigirRol(resolutor, Rol.TURISTA)).addPathPatterns("/turista/**");
    }
}
