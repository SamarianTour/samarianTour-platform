package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.TipoNoCoincideConRntException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.DatosTipo;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;

import java.util.UUID;

/** El tipo del prestador y sus campos condicionales: empleados, o modalidades e idiomas del guía (RF-68, RF-69). */
public interface ActualizarTipoYEmpleadosUseCase {

    /**
     * @throws PrestadorNoRegistradoException si todavía no registró su RNT
     * @throws TipoNoCoincideConRntException  si el tipo nuevo no corresponde a la categoría de su RNT
     */
    Prestador actualizar(UUID prestadorId, DatosTipo datosTipo);
}
