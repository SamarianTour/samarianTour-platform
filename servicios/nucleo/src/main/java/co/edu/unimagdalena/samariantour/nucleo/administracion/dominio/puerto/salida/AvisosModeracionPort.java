package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;

/**
 * Envía la decisión al catálogo como aviso de moderación (DN-08). Se llama dentro de la transacción que guarda la
 * decisión: el aviso sale solo si la decisión queda guardada.
 */
public interface AvisosModeracionPort {

    void avisar(DecisionModeracion decision);
}
