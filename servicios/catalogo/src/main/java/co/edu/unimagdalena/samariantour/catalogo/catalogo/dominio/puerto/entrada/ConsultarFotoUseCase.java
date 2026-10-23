package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;

/**
 * Entrega una foto guardada, para que el navegador la muestre (DC-39). Es pública: las fotos solo se ven junto a lo
 * que el prestador publicó.
 */
public interface ConsultarFotoUseCase {

    /**
     * @return la foto en ese tamaño, como JPG
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.FotoNoEncontradaException si no existe o el identificador no es válido
     */
    byte[] consultar(String fotoId, TamanoFoto tamano);
}
