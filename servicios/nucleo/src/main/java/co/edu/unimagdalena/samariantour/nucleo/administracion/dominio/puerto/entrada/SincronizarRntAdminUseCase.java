package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.ResultadoSincronizacionRnt;

/**
 * La sincronización con el RNT (RF-76, DN-36): corre cada semana y el administrador la ejecuta cuando quiera. Los
 * prestadores son los dueños de su estado; la administración solo la dispara.
 */
public interface SincronizarRntAdminUseCase {

    ResultadoSincronizacionRnt sincronizar();
}
