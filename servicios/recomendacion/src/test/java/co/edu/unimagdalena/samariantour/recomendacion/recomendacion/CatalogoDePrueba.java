package co.edu.unimagdalena.samariantour.recomendacion.recomendacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;

import java.util.List;

/**
 * Catálogo para las pruebas: con ofertas y zonas fijas, o caído y sin copia en caché.
 */
public final class CatalogoDePrueba implements CatalogoConsultaPort {

    private final List<OfertaCandidata> ofertas;
    private final List<ZonaCatalogo> zonas;

    private CatalogoDePrueba(List<OfertaCandidata> ofertas, List<ZonaCatalogo> zonas) {
        this.ofertas = ofertas;
        this.zonas = zonas;
    }

    public static CatalogoConsultaPort de(List<OfertaCandidata> ofertas) {
        return new CatalogoDePrueba(ofertas, List.of());
    }

    public static CatalogoConsultaPort de(List<OfertaCandidata> ofertas, List<ZonaCatalogo> zonas) {
        return new CatalogoDePrueba(ofertas, zonas);
    }

    /** No responde y no hay copia en caché: 503 RECOMENDACION_NO_DISPONIBLE. */
    public static CatalogoConsultaPort caido() {
        return new CatalogoDePrueba(null, null);
    }

    @Override
    public List<OfertaCandidata> ofertasVisibles() {
        return disponible(ofertas);
    }

    @Override
    public List<ZonaCatalogo> zonas() {
        return disponible(zonas);
    }

    private static <T> List<T> disponible(List<T> datos) {
        if (datos == null) {
            throw new RecomendacionNoDisponibleException("El catálogo no responde");
        }
        return datos;
    }
}
