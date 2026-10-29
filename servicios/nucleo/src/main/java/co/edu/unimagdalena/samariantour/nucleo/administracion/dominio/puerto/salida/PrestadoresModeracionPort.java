package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import java.util.UUID;

/** Lo que la moderación necesita de los prestadores: si el RNT está activo (RF-79, DC-38, DN-35). */
public interface PrestadoresModeracionPort {

    /** Falso también si el prestador no se ha registrado. */
    boolean estaHabilitado(UUID prestadorId);
}
