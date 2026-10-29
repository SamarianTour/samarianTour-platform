package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto;

/**
 * La cuenta de cobro (RF-74).
 *
 * @param tipo    AHORROS, CORRIENTE, NEQUI o DAVIPLATA
 * @param entidad el banco; para Nequi y Daviplata no hace falta
 * @param numero  el de la cuenta, o el celular de la billetera
 */
public record CuentaCobroRequest(String tipo, String entidad, String numero, String titular) {

    @Override
    public String toString() {
        return "CuentaCobroRequest[tipo=%s]".formatted(tipo);
    }
}
