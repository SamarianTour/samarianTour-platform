package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ResultadoSincronizacion;

/**
 * Compara a los prestadores con el RNT y actualiza su estado (RF-76, DN-36): el que pierde la habilitación sale del
 * catálogo y recibe un aviso (RF-77, RF-78); el que la recupera vuelve. La administración la ejecuta cada semana y
 * cuando el administrador la pide.
 */
public interface SincronizarRntUseCase {

    ResultadoSincronizacion sincronizar();
}
