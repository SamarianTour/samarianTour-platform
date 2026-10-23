package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoFotoSubidaRepository;

import java.util.List;

/**
 * El registro de fotos en memoria. Las pruebas están en el contrato, que también cumple el adaptador de MongoDB.
 */
class FotoSubidaMemoriaAdapterTest implements ContratoFotoSubidaRepository {

    private final FotoSubidaMemoriaAdapter registro = new FotoSubidaMemoriaAdapter(List.of());

    @Override
    public FotoSubidaRepositoryPort registro() {
        return registro;
    }
}
