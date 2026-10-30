package co.edu.unimagdalena.samariantour.seguridadjwt;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Lee las claves RSA del token como las genera {@code infra/generar-claves.sh}: en Base64 de una línea o en PEM. La
 * privada, en PKCS#8, solo la tiene el núcleo; la pública, en X.509, la tienen el catálogo y la recomendación.
 */
public final class ClavesRsa {

    private ClavesRsa() {
    }

    /** La pública del núcleo. @throws IllegalArgumentException si falta o no es una clave pública RSA en X.509 */
    public static RSAPublicKey publica(String texto) {
        return publica(texto, "SEGURIDAD_JWT_CLAVE_PUBLICA");
    }

    /** @param variable la variable de entorno de donde sale, para el mensaje si falta */
    public static RSAPublicKey publica(String texto, String variable) {
        byte[] der = der(texto, "Falta la clave pública (" + variable + "); se genera con infra/generar-claves.sh");
        try {
            return (RSAPublicKey) fabrica().generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | ClassCastException e) {
            throw new IllegalArgumentException("La clave pública del token no es una clave RSA en X.509", e);
        }
    }

    /** La privada del núcleo. @throws IllegalArgumentException si falta o no es una clave privada RSA en PKCS#8 */
    public static RSAPrivateCrtKey privada(String texto) {
        return privada(texto, "NUCLEO_JWT_CLAVE_PRIVADA");
    }

    /** @param variable la variable de entorno de donde sale, para el mensaje si falta */
    public static RSAPrivateCrtKey privada(String texto, String variable) {
        byte[] der = der(texto, "Falta la clave privada (" + variable + "); se genera con infra/generar-claves.sh");
        try {
            return (RSAPrivateCrtKey) fabrica().generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | ClassCastException e) {
            throw new IllegalArgumentException("La clave del token no es una clave privada RSA en PKCS#8", e);
        }
    }

    /** La pública que corresponde a la privada: el núcleo valida sus propios tokens sin otra variable. */
    public static RSAPublicKey publicaDe(RSAPrivateCrtKey privada) {
        try {
            return (RSAPublicKey) fabrica().generatePublic(
                    new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("La clave del token no es una clave RSA válida", e);
        }
    }

    private static byte[] der(String texto, String siFalta) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(siFalta);
        }
        String base64 = texto.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La clave del token no está en Base64", e);
        }
    }

    private static KeyFactory fabrica() throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA");
    }
}
