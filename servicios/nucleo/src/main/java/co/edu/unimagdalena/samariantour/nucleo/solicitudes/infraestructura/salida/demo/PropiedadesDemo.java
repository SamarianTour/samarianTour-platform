package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

/**
 * PROVISIONAL [SUPUESTO] (DN-15): los prestadores, su operador y su WhatsApp, que tendrá el módulo de prestadores,
 * todavía vacío. Se llenan en {@code application-demo.yml} con los identificadores de la demo del catálogo; sin el
 * perfil demo quedan vacíos y ninguna solicitud se puede enviar. El contacto del turista ya sale de su cuenta (DN-22).
 */
@ConfigurationProperties("nucleo.demo")
public record PropiedadesDemo(List<Prestador> prestadores) {

    public PropiedadesDemo {
        prestadores = prestadores == null ? List.of() : List.copyOf(prestadores);
    }

    /**
     * @param operadorId vacío si el prestador no tiene un operador asignado
     * @param whatsapp   ficticio
     */
    public record Prestador(UUID id, UUID operadorId, String whatsapp) {
    }
}
