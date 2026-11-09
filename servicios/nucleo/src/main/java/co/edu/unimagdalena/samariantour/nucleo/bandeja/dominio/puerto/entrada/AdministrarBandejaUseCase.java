package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;

import java.util.List;

/** Lo que el administrador hace con la bandeja (DN-44): ver los rechazados y reenviar uno. */
public interface AdministrarBandejaUseCase {

    /** Del más reciente al más antiguo. */
    List<AvisoSaliente> rechazados();

    /**
     * Vuelve a dejar pendiente un aviso rechazado.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.excepcion.AvisoNoReenviableException si no existe
     *         o no está rechazado
     */
    AvisoSaliente reenviar(String idAviso);
}
