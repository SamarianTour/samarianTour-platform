package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.ContratoOpinionRepository;

/**
 * Los adaptadores en memoria con los datos de demostración. Las pruebas están en el contrato, que también cumplen los
 * adaptadores de MongoDB.
 */
class OpinionMemoriaAdapterTest implements ContratoOpinionRepository {

    private final OpinionMemoriaAdapter opiniones = new OpinionMemoriaAdapter();
    private final HabilitacionOpinionMemoriaAdapter habilitaciones = new HabilitacionOpinionMemoriaAdapter();

    @Override
    public OpinionRepositoryPort opiniones() {
        return opiniones;
    }

    @Override
    public HabilitacionOpinionRepositoryPort habilitaciones() {
        return habilitaciones;
    }
}
