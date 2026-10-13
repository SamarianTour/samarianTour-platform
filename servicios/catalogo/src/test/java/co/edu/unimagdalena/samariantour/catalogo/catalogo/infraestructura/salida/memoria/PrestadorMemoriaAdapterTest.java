package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoPrestadorRepository;

import java.util.List;

/**
 * La réplica de habilitación en memoria. Las pruebas están en el contrato, que también cumple el adaptador de MongoDB.
 */
class PrestadorMemoriaAdapterTest implements ContratoPrestadorRepository {

    private final PrestadorMemoriaAdapter prestadores =
            new PrestadorMemoriaAdapter(List.of(new PrestadorCatalogo(PRESTADOR, true, 2)));

    @Override
    public PrestadorCatalogoRepositoryPort prestadores() {
        return prestadores;
    }
}
