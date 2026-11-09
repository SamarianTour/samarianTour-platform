package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import java.util.UUID;

/** Avisa a los demás módulos que el prestador registró o cambió su cuenta de cobro (DN-44). */
public interface AvisoCuentaCobroPort {

    void cuentaCambiada(UUID prestadorId);
}
