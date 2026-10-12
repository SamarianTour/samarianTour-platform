package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.Solicitante;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.IdentidadPrestador;

/**
 * Convierte la identidad de la petición en el solicitante de los casos de uso del prestador.
 */
public final class SolicitanteWebMapper {

    private SolicitanteWebMapper() {
    }

    public static Solicitante aSolicitante(IdentidadPrestador identidad) {
        return new Solicitante(identidad.prestadorId(),
                Conversiones.enumerado(identidad.tipoPrestador(), "tipoPrestador", TipoPrestador.class));
    }
}
