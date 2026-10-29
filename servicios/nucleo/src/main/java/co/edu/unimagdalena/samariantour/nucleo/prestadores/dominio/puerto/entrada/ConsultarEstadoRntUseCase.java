package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;

import java.util.UUID;

/** El prestador ve sus datos y su estado RNT con la fecha de la última sincronización (RF-73). */
public interface ConsultarEstadoRntUseCase {

    /** @throws PrestadorNoRegistradoException si todavía no registró su RNT */
    Prestador consultar(UUID prestadorId);
}
