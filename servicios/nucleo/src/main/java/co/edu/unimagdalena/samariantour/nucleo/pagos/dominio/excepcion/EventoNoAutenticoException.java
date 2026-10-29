package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

/**
 * El evento del webhook no trae una firma válida de la pasarela, o no se puede leer (RF-50): responde 401 y no se
 * guarda nada.
 */
public class EventoNoAutenticoException extends ExcepcionDominio {

    public EventoNoAutenticoException(String motivo) {
        super("EVENTO_NO_AUTENTICO", "El evento no viene de la pasarela: " + motivo);
    }
}
