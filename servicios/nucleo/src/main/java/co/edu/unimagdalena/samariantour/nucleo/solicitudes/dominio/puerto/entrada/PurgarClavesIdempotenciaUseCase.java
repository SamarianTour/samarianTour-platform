package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

/** Borra las claves de idempotencia vencidas, que ya no protegen nada (DN-12). */
public interface PurgarClavesIdempotenciaUseCase {

    /** @return cuántas borró */
    int purgar();
}
