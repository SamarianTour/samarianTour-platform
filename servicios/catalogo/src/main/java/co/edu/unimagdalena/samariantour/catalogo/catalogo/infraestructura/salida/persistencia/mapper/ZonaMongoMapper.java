package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;

/**
 * Convierte entre {@link Zona} y {@link ZonaDocument}.
 */
public final class ZonaMongoMapper {

    private ZonaMongoMapper() {
    }

    public static ZonaDocument aDocumento(Zona zona) {
        return new ZonaDocument(ConversionesMongo.referencia(zona.id()), zona.nombre(), zona.descripcion(),
                ConversionesMongo.punto(zona.ubicacion()), zona.areaProtegida(), zona.altaAfluencia(), zona.activa(),
                zona.buenasPracticas(), zona.condicionesVisita());
    }

    public static Zona aDominio(ZonaDocument documento) {
        return new Zona(documento.id().toHexString(), documento.nombre(), documento.descripcion(),
                ConversionesMongo.coordenada(documento.ubicacion()), documento.areaProtegida(),
                documento.altaAfluencia(), documento.activa(), ConversionesMongo.lista(documento.buenasPracticas()),
                documento.condicionesVisita());
    }
}
