package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/**
 * El usuario aceptó una versión de la política que ya no es la vigente: tiene que leer y aceptar la nueva (RF-09,
 * RF-10).
 */
public class PoliticaNoVigenteException extends ExcepcionDominio {

    public PoliticaNoVigenteException(String aceptada, String vigente) {
        super("POLITICA_NO_VIGENTE", "Se aceptó la versión %s de la política de datos, pero la vigente es la %s"
                .formatted(aceptada, vigente));
    }
}
