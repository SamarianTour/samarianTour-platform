package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoOfertaRepository;

/**
 * El adaptador en memoria con los datos de demostración de Santa Marta. Las pruebas están en el contrato, que también
 * cumple el adaptador de MongoDB.
 */
class OfertaMemoriaAdapterTest implements ContratoOfertaRepository {

    private final OfertaMemoriaAdapter ofertas = new OfertaMemoriaAdapter();

    @Override
    public OfertaRepositoryPort ofertas() {
        return ofertas;
    }
}
