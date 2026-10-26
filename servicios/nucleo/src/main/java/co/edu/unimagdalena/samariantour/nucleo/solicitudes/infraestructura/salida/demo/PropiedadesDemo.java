package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

/**
 * PROVISIONAL [SUPUESTO] (DN-15): los prestadores, su operador y los contactos que tendrán los módulos de prestadores y
 * de cuentas, que todavía están vacíos. Se llenan en {@code application-demo.yml} con los identificadores de la demo
 * del catálogo; sin el perfil demo quedan vacíos y ninguna solicitud se puede enviar.
 */
@ConfigurationProperties("nucleo.demo")
public record PropiedadesDemo(List<Prestador> prestadores, List<Turista> turistas) {

    public PropiedadesDemo {
        prestadores = prestadores == null ? List.of() : List.copyOf(prestadores);
        turistas = turistas == null ? List.of() : List.copyOf(turistas);
    }

    /**
     * @param operadorId vacío si el prestador no tiene un operador asignado
     * @param whatsapp   ficticio
     */
    public record Prestador(UUID id, UUID operadorId, String whatsapp) {
    }

    /** @param whatsapp ficticio, verificado (RF-03) */
    public record Turista(UUID id, String nombre, String whatsapp) {
    }
}
