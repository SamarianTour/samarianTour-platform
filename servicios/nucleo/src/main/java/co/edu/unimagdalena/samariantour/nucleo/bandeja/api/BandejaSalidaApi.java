package co.edu.unimagdalena.samariantour.nucleo.bandeja.api;

/**
 * Lo único que los demás módulos usan de la bandeja de salida (DN-06). Se llama desde el adaptador de salida del
 * módulo que origina el aviso, dentro de su transacción: el aviso se guarda junto con el cambio y la bandeja lo envía
 * al catálogo después, hasta que lo recibe.
 */
public interface BandejaSalidaApi {

    /**
     * @param idAviso identificador estable del aviso: el catálogo aplica una sola vez cada uno, así que reenviar el
     *                mismo cambio debe dar el mismo identificador
     * @param cuerpo  el aviso como lo describe el contrato del catálogo, incluidos {@code idAviso} y {@code tipo};
     *                se envía en JSON
     * @return false si ya había un aviso con ese identificador; no se duplica
     */
    boolean encolar(String idAviso, DestinoAviso destino, Object cuerpo);
}
