package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Validaciones;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoHabilitacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoNucleoCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.AvisoRequest;

/**
 * Convierte el aviso del núcleo en el comando del caso de uso (DC-04).
 */
public final class AvisoWebMapper {

    private AvisoWebMapper() {
    }

    public static AvisoNucleoCommand aComando(AvisoRequest aviso) {
        return switch (Conversiones.enumerado(aviso.tipo(), "tipo", TipoAviso.class)) {
            case MODERACION -> new AvisoModeracion(aviso.idAviso(), aviso.ofertaId(),
                    Validaciones.presente(aviso.numeroVersion(), "numeroVersion"),
                    Conversiones.enumerado(aviso.decision(), "decision", DecisionModeracion.class), aviso.motivo());
            case HABILITACION -> new AvisoHabilitacion(Validaciones.texto(aviso.idAviso(), "idAviso"),
                    Conversiones.uuid(aviso.prestadorId(), "prestadorId"),
                    Validaciones.presente(aviso.rntActivo(), "rntActivo"), version(aviso.version()));
        };
    }

    private static long version(Long version) {
        if (Validaciones.presente(version, "version") < 1) {
            throw new DatoInvalidoException("version", "la versión del prestador empieza en 1");
        }
        return version;
    }

    /** Tipos de aviso que el núcleo envía al catálogo. */
    private enum TipoAviso {
        MODERACION,
        HABILITACION
    }
}
