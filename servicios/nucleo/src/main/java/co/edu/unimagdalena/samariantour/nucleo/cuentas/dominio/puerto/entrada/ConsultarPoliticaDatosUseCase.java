package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;

/** Qué versión de la política de datos hay que leer y aceptar para registrarse (RF-09 a RF-11). */
public interface ConsultarPoliticaDatosUseCase {

    PoliticaDatos vigente();
}
