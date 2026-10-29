package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import java.util.UUID;

/** Lo que el catálogo tiene que saber de las cuentas (DN-06). */
public interface AvisosCatalogoPort {

    /**
     * El turista suprimió sus datos: el catálogo borra sus opiniones y sus habilitaciones para opinar (RF-13, DC-45).
     * Se encola en la misma transacción que la supresión.
     */
    void avisarSupresion(UUID turistaId);
}
