package co.edu.unimagdalena.samariantour.seguridadjwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClavesRsaTest {

    @Test
    @DisplayName("lee las claves en Base64 o en PEM, y la pública derivada de la privada es la misma")
    void leeLasClaves() {
        RSAPublicKey publica = ClavesRsa.publica(TokensDePrueba.CLAVE_PUBLICA);
        String pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(publica.getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        assertThat(ClavesRsa.publica(pem)).isEqualTo(publica);
        assertThat(ClavesRsa.publicaDe(ClavesRsa.privada(TokensDePrueba.CLAVE_PRIVADA))).isEqualTo(publica);
    }

    @Test
    @DisplayName("sin clave o con una que no es RSA no arranca, y el mensaje dice cómo generarla")
    void claveFaltanteOInvalida() {
        assertThatThrownBy(() -> ClavesRsa.publica(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SEGURIDAD_JWT_CLAVE_PUBLICA")
                .hasMessageContaining("infra/generar-claves.sh");
        assertThatThrownBy(() -> ClavesRsa.privada(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NUCLEO_JWT_CLAVE_PRIVADA");
        assertThatThrownBy(() -> ClavesRsa.publica("bm8tZXMtdW5hLWNsYXZl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X.509");
        assertThatThrownBy(() -> ClavesRsa.publica(TokensDePrueba.CLAVE_PRIVADA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
