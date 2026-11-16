package co.edu.unimagdalena.samariantour.seguridadjwt;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * El registro de auditoría de los accesos denegados (QAS-04: "el intento queda en el registro de auditoría"). Cada
 * intento deja una línea con el prefijo {@value #PREFIJO}, que se filtra con
 * {@code docker compose logs nucleo | grep AUDITORIA}:
 * <pre>
 * [AUDITORIA QAS-04] acceso denegado codigo=ACCESO_DENEGADO usuario=7000...0007 rol=TURISTA metodo=GET
 *     ruta=/api/nucleo/admin/cuentas-internas ip=203.0.113.7
 * </pre>
 * Lo llaman el 403 de Spring Security ({@link RespuestasSeguridad}) y los manejadores de errores de cada servicio
 * cuando rechazan un recurso ajeno, aunque respondan 404 para no revelar que existe.
 * <p>
 * Solo datos que sirven para investigar y no exponen a nadie: el identificador de la cuenta (nunca el nombre ni el
 * correo), el rol, el método, la ruta sin la consulta (que puede llevar un token, como el de restablecimiento) y la
 * IP. Nunca el token. La IP es el último valor de {@code X-Forwarded-For}, el que agrega Nginx: los anteriores los
 * escribe el cliente y podría inventarlos. Sin ese header (sin Nginx, en las pruebas), la del que se conectó.
 */
public final class RegistroAccesos {

    public static final String PREFIJO = "[AUDITORIA QAS-04]";

    private static final Logger LOG = LoggerFactory.getLogger(RegistroAccesos.class);
    private static final int LARGO_MAXIMO_RUTA = 200;

    private RegistroAccesos() {
    }

    /**
     * Registra un acceso denegado con la identidad de la petición en curso.
     *
     * @param codigo el código del error que recibió quien lo intentó, como {@code ACCESO_DENEGADO} o
     *               {@code SOLICITUD_NO_ENCONTRADA}
     */
    public static void denegado(HttpServletRequest peticion, String codigo) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String usuario = "anonimo";
        String rol = "-";
        if (autenticacion instanceof AutenticacionToken token) {
            usuario = token.getPrincipal().usuarioId().toString();
            rol = token.getPrincipal().rol();
        } else if (autenticacion instanceof JwtAuthenticationToken servicio) {
            usuario = servicio.getName();
            rol = "SERVICIO";
        } else if (autenticacion != null && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            usuario = autenticacion.getName();
        }
        LOG.warn("{} acceso denegado codigo={} usuario={} rol={} metodo={} ruta={} ip={}", PREFIJO, limpiar(codigo),
                limpiar(usuario), limpiar(rol), limpiar(peticion.getMethod()), ruta(peticion), ip(peticion));
    }

    private static String ruta(HttpServletRequest peticion) {
        String ruta = limpiar(peticion.getRequestURI());
        return ruta.length() > LARGO_MAXIMO_RUTA ? ruta.substring(0, LARGO_MAXIMO_RUTA) + "..." : ruta;
    }

    private static String ip(HttpServletRequest peticion) {
        String reenviada = peticion.getHeader("X-Forwarded-For");
        String ip = peticion.getRemoteAddr();
        if (reenviada != null && !reenviada.isBlank()) {
            ip = reenviada.substring(reenviada.lastIndexOf(',') + 1).trim();
        }
        // Una IPv4 o IPv6 solo tiene estos caracteres: lo demás no es una IP y no entra al log.
        return ip == null || !ip.matches("[0-9A-Fa-f:.]{1,45}") ? "desconocida" : ip;
    }

    /** Sin saltos de línea ni otros caracteres de control, para que nadie pueda escribir líneas falsas en el log. */
    private static String limpiar(String texto) {
        return texto == null ? "-" : texto.replaceAll("\\p{Cntrl}", "_");
    }
}
