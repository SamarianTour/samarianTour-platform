package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.TipoCuenta;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

/**
 * PROVISIONAL [SUPUESTO] (DN-26): la cuenta de cobro vigente y el WhatsApp de cada prestador, y el WhatsApp del
 * administrador, que tendrán los módulos de prestadores y de cuentas, todavía vacíos. Se llenan en
 * {@code application-demo.yml}, en la misma tabla de prestadores de las solicitudes (DN-15); sin el perfil demo quedan
 * vacíos y ninguna dispersión encuentra cuenta.
 */
@ConfigurationProperties("nucleo.demo")
public record PropiedadesDemoPagos(List<Prestador> prestadores, Administrador administrador) {

    public PropiedadesDemoPagos {
        prestadores = prestadores == null ? List.of() : List.copyOf(prestadores);
    }

    /**
     * @param whatsapp ficticio
     * @param cuenta   vacía si el prestador no ha registrado una
     */
    public record Prestador(UUID id, String whatsapp, Cuenta cuenta) {
    }

    /** @param numero ficticio; nunca sale completo de la dispersión (RF-75) */
    public record Cuenta(TipoCuenta tipo, String entidad, String numero) {
    }

    /** @param whatsapp ficticio */
    public record Administrador(String whatsapp) {
    }
}
