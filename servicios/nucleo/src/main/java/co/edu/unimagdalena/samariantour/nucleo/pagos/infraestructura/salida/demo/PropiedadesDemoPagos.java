package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PROVISIONAL [SUPUESTO] (DN-26): el WhatsApp del administrador, que recibe los pagos tardíos (RF-50). La cuenta
 * de administrador no tiene WhatsApp en el diccionario de clases; mientras se decide, sale de
 * {@code application-demo.yml}. La cuenta de cobro y el WhatsApp de los prestadores ya salen de su módulo (DN-35).
 */
@ConfigurationProperties("nucleo.demo")
public record PropiedadesDemoPagos(Administrador administrador) {

    /** @param whatsapp ficticio */
    public record Administrador(String whatsapp) {
    }
}
