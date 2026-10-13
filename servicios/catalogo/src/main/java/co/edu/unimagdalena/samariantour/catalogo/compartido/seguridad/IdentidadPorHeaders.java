package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SUSTITUTO DEL TOKEN (DC-01). Mientras no exista libs/seguridad-jwt, toma la identidad del prestador de los headers
 * {@code X-Prestador-Id} y {@code X-Tipo-Prestador}. Cualquiera puede enviarlos, así que solo se activa
 * con el perfil {@code demo}; nunca debe usarse fuera de una demostración.
 */
@Component
@Profile("demo")
public class IdentidadPorHeaders implements ResolutorIdentidad {

    public static final String HEADER_PRESTADOR = "X-Prestador-Id";
    public static final String HEADER_TIPO = "X-Tipo-Prestador";

    @Override
    public IdentidadPrestador prestadorDe(HttpServletRequest peticion) {
        String tipo = peticion.getHeader(HEADER_TIPO);
        if (tipo == null || tipo.isBlank()) {
            throw new NoAutenticadoException("Falta el header " + HEADER_TIPO);
        }
        return new IdentidadPrestador(prestadorId(peticion.getHeader(HEADER_PRESTADOR)), tipo.strip());
    }

    private static UUID prestadorId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new NoAutenticadoException("Falta el header " + HEADER_PRESTADOR);
        }
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw new NoAutenticadoException("El header " + HEADER_PRESTADOR + " no es un identificador válido");
        }
    }
}
