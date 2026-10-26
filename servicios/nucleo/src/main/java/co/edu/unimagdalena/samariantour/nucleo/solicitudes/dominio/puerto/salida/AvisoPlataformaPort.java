package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.AvisoPlataforma;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;

import java.util.UUID;

/**
 * Los avisos del operador dentro de la plataforma (RF-34). Son las notificaciones con el canal PLATAFORMA: el mismo
 * registro, consultable por el operador [SUPUESTO] (DN-13).
 */
public interface AvisoPlataformaPort {

    /** Los avisos del operador, del más reciente al más antiguo. */
    Pagina<AvisoPlataforma> avisosDe(UUID operadorId, Paginacion paginacion);
}
