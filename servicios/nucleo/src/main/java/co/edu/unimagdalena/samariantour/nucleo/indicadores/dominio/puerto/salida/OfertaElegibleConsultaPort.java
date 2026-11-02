package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertasElegibles;

import java.util.List;

/** Las ofertas visibles de cada prestador, que cuenta el catálogo (RET-02, DC-51). */
public interface OfertaElegibleConsultaPort {

    /** @throws CatalogoNoDisponibleException si el catálogo no responde en 3 s o responde un error */
    List<OfertasElegibles> porPrestador();
}
