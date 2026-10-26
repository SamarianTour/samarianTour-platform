package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ContactoTurista;

import java.util.Optional;
import java.util.UUID;

/**
 * El contacto del turista, que es del módulo de cuentas (RF-03). Mientras ese módulo no exista, lo da un adaptador
 * provisional con una tabla de la demo [SUPUESTO] (DN-15).
 */
public interface ContactoPort {

    /** @return vacío si el turista no tiene un WhatsApp verificado: no puede enviar solicitudes (RF-30) */
    Optional<ContactoTurista> turista(UUID turistaId);
}
