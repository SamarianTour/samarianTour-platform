package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import java.time.Instant;
import java.util.UUID;

/**
 * El registro de auditoría de los cambios delicados del prestador (RF-74, RNF-09): quién cambió su cuenta de cobro y
 * cuándo. Nunca guarda el número completo.
 */
public interface AuditoriaPort {

    void registrar(UUID prestadorId, String accion, String detalle, Instant ocurridoEn);
}
