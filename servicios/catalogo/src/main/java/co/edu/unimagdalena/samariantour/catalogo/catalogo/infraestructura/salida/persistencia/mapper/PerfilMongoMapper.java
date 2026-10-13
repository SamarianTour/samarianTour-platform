package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import org.bson.types.ObjectId;

/**
 * Convierte entre {@link PerfilPublico} y {@link PerfilDocument}. El dominio identifica el perfil por su prestador;
 * el {@code _id} del documento solo existe en la base.
 */
public final class PerfilMongoMapper {

    private PerfilMongoMapper() {
    }

    /** @param id el {@code _id} del documento: el del perfil ya guardado, o uno nuevo */
    public static PerfilDocument aDocumento(ObjectId id, PerfilPublico perfil) {
        return new PerfilDocument(id, perfil.prestadorId(), perfil.nombreComercial(), perfil.descripcion(),
                perfil.fotos(), perfil.horariosAtencion(), perfil.sitioWeb(), perfil.redesSociales());
    }

    public static PerfilPublico aDominio(PerfilDocument documento) {
        return new PerfilPublico(documento.prestadorId(), documento.nombreComercial(), documento.descripcion(),
                ConversionesMongo.lista(documento.fotos()), documento.horariosAtencion(), documento.sitioWeb(),
                ConversionesMongo.lista(documento.redesSociales()));
    }
}
