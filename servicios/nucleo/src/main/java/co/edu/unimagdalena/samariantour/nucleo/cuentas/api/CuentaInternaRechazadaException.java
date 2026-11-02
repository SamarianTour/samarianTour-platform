package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

/** Las cuentas rechazaron la operación; con {@code DATO_INVALIDO} el mensaje empieza por el campo. */
public class CuentaInternaRechazadaException extends RuntimeException {

    private final String codigo;

    public CuentaInternaRechazadaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    /** DATO_INVALIDO, CORREO_YA_REGISTRADO, CORREO_NO_DISPONIBLE o CUENTA_NO_ENCONTRADA. */
    public String codigo() {
        return codigo;
    }
}
