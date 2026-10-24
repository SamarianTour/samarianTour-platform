package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DatosZona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.util.List;

/**
 * El administrador gestiona las zonas (HU-ADM-06, DC-49). El nombre único lo garantiza el repositorio, que lo
 * comprueba aunque dos réplicas guarden a la vez.
 */
public class GestionarZonasService implements GestionarZonasUseCase {

    private final ZonaRepositoryPort zonas;

    public GestionarZonasService(ZonaRepositoryPort zonas) {
        this.zonas = zonas;
    }

    @Override
    public List<Zona> listarTodas() {
        return zonas.buscarTodas();
    }

    @Override
    public Zona registrar(DatosZona datos) {
        Zona zona = Zona.registrar(zonas.nuevoId(), datos);
        zonas.guardar(zona);
        return zona;
    }

    @Override
    public Zona editar(String zonaId, DatosZona datos, boolean activa) {
        Zona editada = zonas.buscarPorId(zonaId).orElseThrow(() -> new ZonaNoEncontradaException(zonaId))
                .editar(datos, activa);
        zonas.guardar(editada);
        return editada;
    }
}
