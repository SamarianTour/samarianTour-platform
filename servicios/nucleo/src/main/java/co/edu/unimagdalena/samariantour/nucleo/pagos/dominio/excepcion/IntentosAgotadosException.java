package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.PoliticaReintentos;

/** La solicitud ya tuvo los tres intentos de pago con resultado de la pasarela (RF-58). */
public class IntentosAgotadosException extends ExcepcionDominio {

    public IntentosAgotadosException() {
        super("INTENTOS_AGOTADOS", "La solicitud ya tuvo los %d intentos de pago que permite la plataforma"
                .formatted(PoliticaReintentos.INTENTOS_PAGO));
    }
}
