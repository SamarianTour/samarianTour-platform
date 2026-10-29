package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import java.util.UUID;

/**
 * Lo que el catálogo tiene que saber de los prestadores (RF-77, DC-02): su habilitación, que decide si sus ofertas son
 * visibles. Se encola en la bandeja de salida, en la misma transacción que el cambio (DN-06).
 */
public interface EventoSalidaPort {

    /** @param version la de la habilitación del prestador: el catálogo ignora una igual o menor a la que tiene */
    void avisarHabilitacion(UUID prestadorId, boolean rntActivo, long version);
}
