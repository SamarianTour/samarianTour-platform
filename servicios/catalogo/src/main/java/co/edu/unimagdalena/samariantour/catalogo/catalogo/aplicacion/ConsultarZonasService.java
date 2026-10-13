package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.util.List;

/**
 * Zonas activas (RF-14, RF-25).
 */
public class ConsultarZonasService implements ConsultarZonasUseCase {

    private final ZonaRepositoryPort zonas;

    public ConsultarZonasService(ZonaRepositoryPort zonas) {
        this.zonas = zonas;
    }

    @Override
    public List<Zona> consultarActivas() {
        return zonas.buscarActivas();
    }
}
