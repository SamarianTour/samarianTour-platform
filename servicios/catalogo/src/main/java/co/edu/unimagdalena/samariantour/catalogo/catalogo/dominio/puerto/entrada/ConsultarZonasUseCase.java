package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.util.List;

/**
 * Zonas activas, para filtrar la búsqueda y elegir la zona de una oferta (RF-14, RF-25).
 */
public interface ConsultarZonasUseCase {

    List<Zona> consultarActivas();
}
