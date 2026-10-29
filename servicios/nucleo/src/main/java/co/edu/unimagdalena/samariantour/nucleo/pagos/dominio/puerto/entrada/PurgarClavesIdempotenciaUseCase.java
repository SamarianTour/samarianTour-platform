package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

/** Borra las claves de idempotencia de pagos que ya vencieron (DN-19). */
public interface PurgarClavesIdempotenciaUseCase {

    /** @return cuántas borró */
    int purgar();
}
