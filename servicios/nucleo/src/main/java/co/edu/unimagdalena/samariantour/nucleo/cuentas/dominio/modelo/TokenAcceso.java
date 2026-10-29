package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Instant;
import java.util.Objects;

/**
 * El token de acceso firmado (JWT RS256) y cuándo vence (DN-29). Los servicios lo validan con la clave pública, sin
 * consultar al núcleo.
 */
public record TokenAcceso(String valor, Instant expiraEn) {

    public TokenAcceso {
        Objects.requireNonNull(valor, "valor");
        Objects.requireNonNull(expiraEn, "expiraEn");
    }

    @Override
    public String toString() {
        return "TokenAcceso[expiraEn=%s]".formatted(expiraEn);
    }
}
