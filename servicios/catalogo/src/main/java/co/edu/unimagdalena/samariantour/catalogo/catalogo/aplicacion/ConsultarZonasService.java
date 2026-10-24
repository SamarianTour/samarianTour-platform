package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.util.List;

/**
 * Zonas activas y ficha de una zona (RF-14, RF-25).
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

    @Override
    public Zona consultar(String zonaId) {
        return zonas.buscarPorId(zonaId).orElseThrow(() -> new ZonaNoEncontradaException(zonaId));
    }
}
