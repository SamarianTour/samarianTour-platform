package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

/**
 * Ubicación geográfica; tipo de valor reutilizable por zonas y ofertas.
 */
public record Coordenada(double latitud, double longitud) {

    public Coordenada {
        if (latitud < -90 || latitud > 90) {
            throw new DatoInvalidoException("latitud", "debe estar entre -90 y 90");
        }
        if (longitud < -180 || longitud > 180) {
            throw new DatoInvalidoException("longitud", "debe estar entre -180 y 180");
        }
    }
}
