package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Sin el perfil {@code demo} no hay forma de identificar al usuario: el JWT aún no existe y los headers
 * que lo sustituyen no se aceptan. Toda petición que necesite identidad se rechaza.
 */
@Component
@Profile("!demo")
public class IdentidadNoDisponible implements ResolutorIdentidad {

    @Override
    public Identidad identidadDe(HttpServletRequest peticion) {
        throw new NoAutenticadoException(
                "La autenticación con JWT aún no está disponible; los headers de demostración exigen el perfil demo");
    }
}
