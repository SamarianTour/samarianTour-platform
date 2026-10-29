package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.ResultadoSincronizacionRnt;

/** La sincronización con el RNT que hacen los prestadores (RF-76 a RF-78, DN-36). */
public interface SincronizacionRntPort {

    ResultadoSincronizacionRnt sincronizar();
}
