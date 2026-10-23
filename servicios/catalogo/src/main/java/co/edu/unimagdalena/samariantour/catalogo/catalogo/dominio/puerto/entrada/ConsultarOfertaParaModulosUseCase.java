package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import java.util.Collection;
import java.util.Map;

/**
 * Consultas de los otros módulos del servicio sobre las ofertas: existencia, visibilidad y nombre (DC-40). Las
 * expone {@code catalogo.api.ConsultaOfertaApi}.
 */
public interface ConsultarOfertaParaModulosUseCase {

    /** Si la oferta existe, en cualquier estado. */
    boolean existe(String ofertaId);

    /** Si la oferta es visible, con la misma regla que la ficha: un paquete exige además sus actividades (RF-22). */
    boolean esVisible(String ofertaId);

    /** Nombre de la versión publicada de cada oferta, o el de la pendiente si no tiene publicada; omite las que no existen. */
    Map<String, String> nombres(Collection<String> ofertaIds);
}
