package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilPublicoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilRequest;

import java.util.UUID;

/**
 * Convierte el perfil público (RF-24, RF-64, DC-39); sus fotos, con {@link FotoWebMapper}.
 */
public final class PerfilWebMapper {

    private PerfilWebMapper() {
    }

    public static GuardarPerfilCommand aComando(UUID prestadorId, PerfilRequest peticion) {
        return new GuardarPerfilCommand(prestadorId, peticion.nombreComercial(), peticion.descripcion(),
                peticion.horariosAtencion(), peticion.sitioWeb(), peticion.redesSociales());
    }

    public static PerfilPublicoResponse aRespuesta(PerfilPublico perfil) {
        return new PerfilPublicoResponse(perfil.nombreComercial(), perfil.descripcion(),
                FotoWebMapper.aRespuestas(perfil.fotos()), perfil.horariosAtencion(), perfil.sitioWeb(),
                perfil.redesSociales());
    }
}
