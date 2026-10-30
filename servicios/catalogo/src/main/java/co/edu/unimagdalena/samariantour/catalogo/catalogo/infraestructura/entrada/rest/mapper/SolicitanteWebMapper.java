package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.Solicitante;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.AccesoDenegadoException;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.Identidad;

/**
 * Convierte la identidad de la petición en el solicitante de los casos de uso del prestador. El tipo sale del token: un
 * prestador que todavía no registró su RNT en el núcleo no lo tiene y no puede gestionar ofertas (403), porque el tipo
 * decide qué ofertas publica. Después de registrarlo, renovar la sesión trae un token con el tipo.
 */
public final class SolicitanteWebMapper {

    private SolicitanteWebMapper() {
    }

    public static Solicitante aSolicitante(Identidad identidad) {
        if (identidad.tipoPrestador() == null) {
            throw new AccesoDenegadoException("Registra tu RNT en el núcleo y renueva la sesión antes de gestionar "
                    + "ofertas: el tipo de prestador decide qué puedes publicar");
        }
        return new Solicitante(identidad.usuarioId(),
                Conversiones.enumerado(identidad.tipoPrestador(), "tipoPrestador", TipoPrestador.class));
    }
}
