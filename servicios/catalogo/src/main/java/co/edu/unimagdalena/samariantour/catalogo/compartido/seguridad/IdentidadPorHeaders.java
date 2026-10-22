package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * SUSTITUTO DEL TOKEN (DC-01). Mientras no exista libs/seguridad-jwt, toma la identidad del usuario de los headers
 * {@code X-Usuario-Id}, {@code X-Rol} y, para el rol PRESTADOR, {@code X-Tipo-Prestador}: los mismos datos que
 * traerá el JWT. Cualquiera puede enviarlos, así que solo se activa con el perfil {@code demo}; nunca debe usarse
 * fuera de una demostración.
 */
@Component
@Profile("demo")
public class IdentidadPorHeaders implements ResolutorIdentidad {

    public static final String HEADER_USUARIO = "X-Usuario-Id";
    public static final String HEADER_ROL = "X-Rol";
    public static final String HEADER_TIPO = "X-Tipo-Prestador";

    @Override
    public Identidad identidadDe(HttpServletRequest peticion) {
        UUID usuarioId = usuarioId(obligatorio(peticion, HEADER_USUARIO));
        Rol rol = rol(obligatorio(peticion, HEADER_ROL));
        String tipoPrestador = rol == Rol.PRESTADOR ? obligatorio(peticion, HEADER_TIPO) : null;
        return new Identidad(usuarioId, rol, tipoPrestador);
    }

    private static String obligatorio(HttpServletRequest peticion, String header) {
        String valor = peticion.getHeader(header);
        if (valor == null || valor.isBlank()) {
            throw new NoAutenticadoException("Falta el header " + header);
        }
        return valor.strip();
    }

    private static UUID usuarioId(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            throw new NoAutenticadoException("El header " + HEADER_USUARIO + " no es un identificador válido");
        }
    }

    private static Rol rol(String valor) {
        return Arrays.stream(Rol.values())
                .filter(rol -> rol.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new NoAutenticadoException("El header " + HEADER_ROL + " no es un rol válido: "
                        + Arrays.toString(Rol.values())));
    }
}
