package co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra en reposo los datos personales (RNF-08, DN-28) con AES-256-GCM: cada valor lleva su propio vector aleatorio y
 * una etiqueta que delata cualquier cambio. El texto guardado es {@code v1:} y el Base64 del vector y el cifrado; el
 * prefijo permite cambiar de algoritmo sin perder lo ya guardado. Lo usan los adaptadores de persistencia de cada
 * módulo; el dominio nunca ve un valor cifrado.
 */
public final class CifradorDatos {

    private static final String PREFIJO = "v1:";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int BYTES_CLAVE = 32;
    private static final int BYTES_VECTOR = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecretKey clave;
    private final SecureRandom azar = new SecureRandom();

    private CifradorDatos(byte[] clave) {
        if (clave.length != BYTES_CLAVE) {
            throw new IllegalArgumentException("La clave de cifrado debe tener %d bytes y tiene %d"
                    .formatted(BYTES_CLAVE, clave.length));
        }
        this.clave = new SecretKeySpec(clave, "AES");
    }

    /** @param claveBase64 32 bytes aleatorios en Base64, como los genera {@code infra/generar-claves.sh} */
    public static CifradorDatos conClave(String claveBase64) {
        if (claveBase64 == null || claveBase64.isBlank()) {
            throw new IllegalArgumentException("Falta la clave de cifrado de los datos personales "
                    + "(NUCLEO_CIFRADO_CLAVE); se genera con infra/generar-claves.sh");
        }
        return new CifradorDatos(Base64.getDecoder().decode(claveBase64.strip()));
    }

    /** @return vacío si el valor es vacío */
    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        byte[] vector = new byte[BYTES_VECTOR];
        azar.nextBytes(vector);
        try {
            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, vector));
            byte[] cifrado = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return PREFIJO + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(vector.length + cifrado.length).put(vector).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato", e);
        }
    }

    /** @throws IllegalStateException si el valor no lo cifró esta clave o lo cambiaron */
    public String descifrar(String guardado) {
        if (guardado == null) {
            return null;
        }
        if (!guardado.startsWith(PREFIJO)) {
            throw new IllegalStateException("El dato no tiene el formato cifrado " + PREFIJO);
        }
        ByteBuffer bytes = ByteBuffer.wrap(Base64.getDecoder().decode(guardado.substring(PREFIJO.length())));
        byte[] vector = new byte[BYTES_VECTOR];
        bytes.get(vector);
        byte[] cifrado = new byte[bytes.remaining()];
        bytes.get(cifrado);
        try {
            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, vector));
            return new String(cifrador.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el dato: otra clave o un valor alterado", e);
        }
    }
}
