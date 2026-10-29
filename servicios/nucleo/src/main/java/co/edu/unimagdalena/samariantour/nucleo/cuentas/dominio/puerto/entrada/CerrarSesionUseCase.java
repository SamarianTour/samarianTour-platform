package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

/**
 * Cierra la sesión: el token de renovación deja de servir (RF-06). El token de acceso ya emitido vence solo, en 15
 * minutos como máximo (DN-29).
 */
public interface CerrarSesionUseCase {

    /** No falla con un token que no sirve: la sesión queda cerrada de todas formas. */
    void cerrar(String tokenRenovacion);
}
