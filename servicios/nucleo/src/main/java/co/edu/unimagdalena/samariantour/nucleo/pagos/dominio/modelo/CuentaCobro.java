package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.util.Objects;

/**
 * La cuenta de cobro vigente de un prestador (RF-74). Solo la usa la dispersión (RF-75): nunca sale completa en una
 * respuesta ni en un log, solo sus últimos 4 dígitos. Por eso {@link #toString()} la enmascara.
 *
 * @param entidad el banco, o la billetera para Nequi y Daviplata
 */
public record CuentaCobro(TipoCuenta tipo, String entidad, String numero) {

    public CuentaCobro {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(entidad, "entidad");
        Objects.requireNonNull(numero, "numero");
    }

    /** Solo los últimos 4 dígitos: {@code 4011223344} queda {@code ******3344}. */
    public String enmascarada() {
        String digitos = numero.replaceAll("\\D", "");
        return digitos.length() <= 4 ? "****" : "******" + digitos.substring(digitos.length() - 4);
    }

    @Override
    public String toString() {
        return "CuentaCobro[" + tipo + " " + entidad + " " + enmascarada() + "]";
    }
}
