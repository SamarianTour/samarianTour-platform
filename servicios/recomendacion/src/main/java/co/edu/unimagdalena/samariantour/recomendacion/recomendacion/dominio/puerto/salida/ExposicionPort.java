package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;

/**
 * Registro de exposición (RET-02, DR-08): qué ofertas se mostraron y en qué posición, sin datos del turista.
 */
public interface ExposicionPort {

    /** Nunca lanza: si el registro falla, la recomendación igual llega al turista. */
    void registrar(RegistroExposicion registro);
}
