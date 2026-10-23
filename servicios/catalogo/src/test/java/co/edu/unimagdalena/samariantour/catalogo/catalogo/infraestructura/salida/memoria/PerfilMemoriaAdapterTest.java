package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.ContratoPerfilRepository;

/**
 * El adaptador en memoria con los perfiles de demostración. Las pruebas están en el contrato, que también cumple el
 * adaptador de MongoDB.
 */
class PerfilMemoriaAdapterTest implements ContratoPerfilRepository {

    private final PerfilMemoriaAdapter perfiles = new PerfilMemoriaAdapter();

    @Override
    public PerfilRepositoryPort perfiles() {
        return perfiles;
    }
}
