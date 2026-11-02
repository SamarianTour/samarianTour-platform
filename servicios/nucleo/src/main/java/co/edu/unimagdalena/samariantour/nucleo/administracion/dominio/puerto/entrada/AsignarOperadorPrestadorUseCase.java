package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorNoValidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;

import java.util.List;
import java.util.UUID;

/** El administrador asigna a cada prestador su operador de disponibilidad (RF-32, RF-81, DN-39). */
public interface AsignarOperadorPrestadorUseCase {

    /**
     * Rige para las solicitudes nuevas; las abiertas siguen con el operador que tenían.
     *
     * @throws OperadorNoValidoException     si no es un operador de disponibilidad activo
     * @throws PrestadorNoRegistradoException si el prestador todavía no registró su RNT
     */
    PrestadorRegistrado asignar(UUID prestadorId, UUID operadorId);

    /** Con {@code soloSinOperador}, solo los que faltan por asignar. */
    List<PrestadorRegistrado> prestadores(boolean soloSinOperador);
}
