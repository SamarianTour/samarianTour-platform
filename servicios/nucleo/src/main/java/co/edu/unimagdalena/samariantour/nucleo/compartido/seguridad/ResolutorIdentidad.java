package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Obtiene la identidad del usuario que hace la petición. Hoy la da un sustituto del token (DN-05);
 * cuando exista libs/seguridad-jwt solo cambia la implementación.
 */
public interface ResolutorIdentidad {

    /** @throws NoAutenticadoException si la petición no trae una identidad válida */
    Identidad identidadDe(HttpServletRequest peticion);
}
