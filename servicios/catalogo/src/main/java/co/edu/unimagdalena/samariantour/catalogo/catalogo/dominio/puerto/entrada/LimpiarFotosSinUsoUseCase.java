package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

/**
 * Borra las fotos que nadie usa (DC-47): las que se subieron y nunca se asignaron, las de versiones reemplazadas y las
 * de ofertas retiradas. Solo borra una foto si pasó el plazo sin uso y ninguna oferta no retirada ni ningún perfil la
 * tiene. La ejecuta una tarea programada.
 */
public interface LimpiarFotosSinUsoUseCase {

    /** @return cuántas fotos borró */
    int limpiar();
}
