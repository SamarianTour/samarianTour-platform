package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import java.util.Optional;
import java.util.UUID;

/** Lo que las solicitudes necesitan de los prestadores, por la API del módulo de prestadores (DN-15, DN-35). */
public interface PrestadoresPort {

    /** El operador vinculado estáticamente al prestador (RF-32); vacío si no tiene uno asignado. */
    Optional<UUID> operadorAsignado(UUID prestadorId);

    /** El WhatsApp por el que el prestador recibe las notificaciones (RF-71); no es público. */
    Optional<String> whatsapp(UUID prestadorId);

    /** Si tiene el RNT activo (RF-77); falso también si no se ha registrado. */
    boolean estaHabilitado(UUID prestadorId);
}
