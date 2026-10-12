package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ZonaResponse;

public final class ZonaWebMapper {

    private ZonaWebMapper() {
    }

    public static ZonaResponse aRespuesta(Zona zona) {
        return new ZonaResponse(zona.id(), zona.nombre(), zona.descripcion(),
                Conversiones.coordenada(zona.ubicacion()), zona.areaProtegida(), zona.altaAfluencia(),
                zona.buenasPracticas(), zona.condicionesVisita());
    }
}
