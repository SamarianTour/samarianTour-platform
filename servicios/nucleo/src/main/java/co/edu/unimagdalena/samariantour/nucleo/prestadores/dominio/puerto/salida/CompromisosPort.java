package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import java.util.List;
import java.util.UUID;

/** Los compromisos pendientes del prestador en los demás módulos (DN-45). */
public interface CompromisosPort {

    /** Vacía si puede suprimir sus datos. */
    List<String> pendientes(UUID prestadorId);
}
