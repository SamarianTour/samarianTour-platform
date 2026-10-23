package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.FotoSubidaDocument;

/**
 * Convierte entre {@link FotoSubida} y {@link FotoSubidaDocument}.
 */
public final class FotoSubidaMongoMapper {

    private FotoSubidaMongoMapper() {
    }

    public static FotoSubidaDocument aDocumento(FotoSubida foto) {
        return new FotoSubidaDocument(foto.foto().id(), foto.prestadorId(), foto.destino(), foto.ultimoUso());
    }

    public static FotoSubida aDominio(FotoSubidaDocument documento) {
        return new FotoSubida(new Foto(documento.id()), documento.prestadorId(), documento.destino(),
                documento.ultimoUso());
    }
}
