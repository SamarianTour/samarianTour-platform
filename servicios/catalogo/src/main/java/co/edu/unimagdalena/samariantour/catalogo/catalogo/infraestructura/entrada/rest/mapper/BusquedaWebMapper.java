package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TextoBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;

import java.math.BigDecimal;

/**
 * Convierte los parámetros de la búsqueda pública en los criterios del dominio (RF-14 a RF-17).
 */
public final class BusquedaWebMapper {

    private BusquedaWebMapper() {
    }

    public static CriteriosBusqueda aCriterios(String texto, String tipo, String zonaId, BigDecimal precioMin,
                                               BigDecimal precioMax, String tipoActividad, String nivelEsfuerzoMaximo,
                                               Boolean accesibleSillaRuedas, Integer pagina, Integer tamano) {
        return new CriteriosBusqueda(
                TextoBusqueda.de(texto),
                Conversiones.enumeradoOpcional(tipo, "tipo", TipoOferta.class),
                zonaId, precioMin, precioMax,
                Conversiones.enumeradoOpcional(tipoActividad, "tipoActividad", TipoActividad.class),
                Conversiones.enumeradoOpcional(nivelEsfuerzoMaximo, "nivelEsfuerzoMaximo", NivelEsfuerzo.class),
                Boolean.TRUE.equals(accesibleSillaRuedas),
                Paginacion.de(pagina, tamano));
    }
}
