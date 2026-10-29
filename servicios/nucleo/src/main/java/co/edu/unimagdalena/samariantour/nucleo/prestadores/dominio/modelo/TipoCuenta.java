package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

/**
 * El tipo de la cuenta de cobro (RF-74). El diccionario dice BANCARIA; aquí se distingue ahorros de corriente, como en
 * pagos, que es quien la usa para dispersar (DN-34).
 */
public enum TipoCuenta {
    AHORROS,
    CORRIENTE,
    NEQUI,
    DAVIPLATA;

    /** Nequi y Daviplata son billeteras: su número es el celular y la entidad es la misma billetera. */
    public boolean esBilletera() {
        return this == NEQUI || this == DAVIPLATA;
    }
}
