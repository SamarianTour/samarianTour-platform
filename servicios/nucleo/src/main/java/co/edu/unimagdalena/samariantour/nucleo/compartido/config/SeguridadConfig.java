package co.edu.unimagdalena.samariantour.nucleo.compartido.config;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.ExigirRol;
import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.NoAutenticadoException;
import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.ResolutorIdentidad;
import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Rol;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.EnumSet;

/**
 * Autorización por prefijo de ruta, en un solo lugar para todos los módulos (DN-05): cada prefijo exige un rol, y
 * {@code /cuenta/**} admite cualquiera.
 * Las demás rutas son públicas, y {@code /internal/**} solo es accesible desde la red interna (Nginx la bloquea
 * hacia afuera).
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
        registro.addInterceptor(new ExigirRol(resolutor, Rol.OPERADOR_DISPONIBILIDAD)).addPathPatterns("/operador/**");
        registro.addInterceptor(new ExigirRol(resolutor, Rol.GESTOR_DESTINO)).addPathPatterns("/gestor/**");
        // Los datos de la propia cuenta, de cualquier rol (RF-12).
        registro.addInterceptor(new ExigirRol(resolutor, EnumSet.allOf(Rol.class))).addPathPatterns("/cuenta/**");
    }
}
