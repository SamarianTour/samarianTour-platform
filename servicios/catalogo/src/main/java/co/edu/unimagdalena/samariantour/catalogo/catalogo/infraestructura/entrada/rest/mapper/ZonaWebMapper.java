package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DatosZona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Validaciones;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ZonaRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ZonaResponse;

public final class ZonaWebMapper {

    private ZonaWebMapper() {
    }

    public static ZonaResponse aRespuesta(Zona zona) {
        return new ZonaResponse(zona.id(), zona.nombre(), zona.descripcion(),
                Conversiones.coordenada(zona.ubicacion()), zona.areaProtegida(), zona.altaAfluencia(), zona.activa(),
                zona.buenasPracticas(), zona.condicionesVisita());
    }

    /** Las marcas que no se envían quedan en false; las reglas de los datos las aplica DatosZona. */
    public static DatosZona aDatos(ZonaRequest peticion) {
        return new DatosZona(peticion.nombre(), peticion.descripcion(), Conversiones.coordenada(peticion.ubicacion()),
                Boolean.TRUE.equals(peticion.areaProtegida()), Boolean.TRUE.equals(peticion.altaAfluencia()),
                peticion.buenasPracticas(), peticion.condicionesVisita());
    }

    /** Al editar, {@code activa} es obligatoria (DC-49). */
    public static boolean activa(ZonaRequest peticion) {
        return Validaciones.presente(peticion.activa(), "activa");
    }
}
