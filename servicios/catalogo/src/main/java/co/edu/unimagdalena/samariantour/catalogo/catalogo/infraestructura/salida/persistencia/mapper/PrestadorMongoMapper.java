package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PrestadorDocument;

/**
 * Convierte entre {@link PrestadorCatalogo} y {@link PrestadorDocument}.
 */
public final class PrestadorMongoMapper {

    private PrestadorMongoMapper() {
    }

    public static PrestadorDocument aDocumento(PrestadorCatalogo prestador) {
        return new PrestadorDocument(prestador.prestadorId(), prestador.rntActivo(), prestador.version());
    }

    public static PrestadorCatalogo aDominio(PrestadorDocument documento) {
        return new PrestadorCatalogo(documento.id(), documento.rntActivo(), documento.version());
    }
}
