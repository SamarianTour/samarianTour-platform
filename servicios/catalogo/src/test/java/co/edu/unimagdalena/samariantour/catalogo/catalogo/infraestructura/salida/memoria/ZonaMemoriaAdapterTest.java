package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoZonaRepository;

/**
 * El adaptador en memoria con las zonas de la precarga. Las pruebas están en el contrato, que también cumple el
 * adaptador de MongoDB.
 */
class ZonaMemoriaAdapterTest implements ContratoZonaRepository {

    private final ZonaMemoriaAdapter zonas = new ZonaMemoriaAdapter();

    @Override
    public ZonaRepositoryPort zonas() {
        return zonas;
    }
}
