package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.util.List;

/**
 * Zonas activas, para filtrar la búsqueda y elegir la zona de una oferta (RF-14, RF-25), y la ficha de una zona.
 */
public interface ConsultarZonasUseCase {

    List<Zona> consultarActivas();

    /**
     * Ficha de la zona con sus buenas prácticas (RF-25, HU-TUR-16). También responde por una zona inactiva, porque sus
     * ofertas siguen visibles (DC-49).
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException si no existe
     */
    Zona consultar(String zonaId);
}
