package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;

/** Las versiones publicadas de la política de tratamiento de datos (RF-10). */
public interface PoliticaDatosRepositoryPort {

    /** La de publicación más reciente. Siempre hay una: la v1 viene con el esquema. */
    PoliticaDatos vigente();
}
