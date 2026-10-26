package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que las solicitudes necesitan de los prestadores, que son del módulo de prestadores. Mientras ese módulo no
 * exista, lo da un adaptador provisional con una tabla de la demo [SUPUESTO] (DN-15).
 */
public interface PrestadoresPort {

    /** El operador vinculado estáticamente al prestador (RF-32); vacío si no tiene uno asignado. */
    Optional<UUID> operadorAsignado(UUID prestadorId);

    /** El WhatsApp por el que el prestador recibe las notificaciones (RF-71); no es público. */
    Optional<String> whatsapp(UUID prestadorId);
}
