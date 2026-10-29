package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import java.util.Optional;
import java.util.UUID;

/**
 * El WhatsApp del administrador y de cada prestador, a quienes avisa pagos (RF-50, RF-54, RF-71). Son de los módulos de
 * cuentas y prestadores, todavía vacíos: por ahora los lee un adaptador provisional de la demo [SUPUESTO] (DN-26).
 */
public interface ContactosPagosPort {

    Optional<String> whatsappAdministrador();

    Optional<String> whatsappPrestador(UUID prestadorId);
}
