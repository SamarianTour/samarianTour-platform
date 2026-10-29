package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/** No hay un código vigente: venció, ya se usó o agotó sus intentos (RF-08). Hay que pedir uno nuevo. */
public class CodigoNoVigenteException extends ExcepcionDominio {

    public CodigoNoVigenteException() {
        super("CODIGO_NO_VIGENTE", "El código venció, ya se usó o agotó sus intentos: pide uno nuevo");
    }
}
