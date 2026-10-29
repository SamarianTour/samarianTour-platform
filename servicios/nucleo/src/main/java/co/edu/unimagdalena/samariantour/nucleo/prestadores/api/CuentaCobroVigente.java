package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

import java.util.Objects;

/**
 * La cuenta de cobro vigente, completa, para la dispersión (RF-54, RF-75). Nunca sale completa en un log:
 * {@link #toString()} la enmascara.
 *
 * @param tipo AHORROS, CORRIENTE, NEQUI o DAVIPLATA
 */
public record CuentaCobroVigente(String tipo, String entidad, String numero) {

    public CuentaCobroVigente {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(entidad, "entidad");
        Objects.requireNonNull(numero, "numero");
    }

    @Override
    public String toString() {
        String fin = numero.length() <= 4 ? "****" : "******" + numero.substring(numero.length() - 4);
        return "CuentaCobroVigente[" + tipo + " " + entidad + " " + fin + "]";
    }
}
