package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * La cuenta donde el prestador recibe la dispersión (RF-74, DN-34). Se conserva el historial: cada cambio crea una
 * cuenta nueva y cierra la anterior. El número y el titular nunca salen completos fuera de la dispersión (RF-75):
 * {@link #toString()} los enmascara.
 *
 * @param vigenteHasta vacío si es la vigente
 */
public record CuentaCobro(UUID id, UUID prestadorId, TipoCuenta tipo, String entidad, String numero, String titular,
                          Instant vigenteDesde, Instant vigenteHasta) {

    public static final int LONGITUD_MAXIMA_TEXTO = 100;
    /** El celular de la billetera, colombiano [SUPUESTO] (DN-34). */
    private static final Pattern CELULAR = Pattern.compile("^3\\d{9}$");
    /** El número de una cuenta bancaria, de 6 a 20 dígitos [SUPUESTO] (DN-34). */
    private static final Pattern CUENTA_BANCARIA = Pattern.compile("^\\d{6,20}$");

    public CuentaCobro {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(prestadorId, "prestadorId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(entidad, "entidad");
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(titular, "titular");
        vigenteDesde = Objects.requireNonNull(vigenteDesde, "vigenteDesde").truncatedTo(ChronoUnit.MICROS);
        vigenteHasta = vigenteHasta == null ? null : vigenteHasta.truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Una cuenta nueva, vigente desde ahora. La billetera usa como entidad su propio nombre; el banco la exige.
     *
     * @throws DatoInvalidoException si un dato no cumple las reglas
     */
    public static CuentaCobro nueva(UUID id, UUID prestadorId, TipoCuenta tipo, String entidad, String numero,
                                    String titular, Instant ahora) {
        if (tipo == null) {
            throw new DatoInvalidoException("tipo", "debe ser AHORROS, CORRIENTE, NEQUI o DAVIPLATA");
        }
        String limpio = numero == null ? "" : numero.replaceAll("[\\s-]", "");
        if (tipo.esBilletera() ? !CELULAR.matcher(limpio).matches() : !CUENTA_BANCARIA.matcher(limpio).matches()) {
            throw new DatoInvalidoException("numero", tipo.esBilletera()
                    ? "el de la billetera es el celular: 10 dígitos que empiezan por 3"
                    : "son de 6 a 20 dígitos");
        }
        String banco = tipo.esBilletera() ? (tipo == TipoCuenta.NEQUI ? "Nequi" : "Daviplata") : texto(entidad,
                "entidad");
        return new CuentaCobro(id, prestadorId, tipo, banco, limpio, texto(titular, "titular"), ahora, null);
    }

    public boolean esVigente() {
        return vigenteHasta == null;
    }

    /** La cierra porque el prestador registró otra (RF-74). */
    public CuentaCobro cerrar(Instant ahora) {
        return new CuentaCobro(id, prestadorId, tipo, entidad, numero, titular, vigenteDesde, ahora);
    }

    /** Si dice lo mismo que otra: registrar otra vez la misma cuenta no cambia nada. */
    public boolean mismosDatos(CuentaCobro otra) {
        return tipo == otra.tipo && entidad.equals(otra.entidad) && numero.equals(otra.numero)
                && titular.equals(otra.titular);
    }

    /** Solo los últimos 4 dígitos: {@code 40112233440101} queda {@code ******0101}. */
    public String numeroEnmascarado() {
        return numero.length() <= 4 ? "****" : "******" + numero.substring(numero.length() - 4);
    }

    private static String texto(String valor, String campo) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty() || limpio.length() > LONGITUD_MAXIMA_TEXTO) {
            throw new DatoInvalidoException(campo, "es obligatorio, de hasta %d caracteres"
                    .formatted(LONGITUD_MAXIMA_TEXTO));
        }
        return limpio;
    }

    @Override
    public String toString() {
        return "CuentaCobro[%s %s %s, vigente desde %s]".formatted(tipo, entidad, numeroEnmascarado(), vigenteDesde);
    }
}
