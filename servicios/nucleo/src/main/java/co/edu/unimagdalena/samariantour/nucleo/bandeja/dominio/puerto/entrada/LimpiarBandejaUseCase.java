package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada;

/** Borra los avisos enviados hace más de 30 días (DN-44). */
public interface LimpiarBandejaUseCase {

    /** @return cuántos borró */
    int limpiar();
}
