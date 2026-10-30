package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Firma tokens de acceso para las pruebas de los tres servicios, como los firma el núcleo. El par de claves se genera
 * al cargar la clase, así que no hay claves en el repositorio; {@link ClavesDePrueba} entrega la pública (y la
 * privada, para el núcleo) a cada contexto de Spring de las pruebas.
 *
 * <pre>{@code
 * http.perform(get("/turista/solicitudes").header(AUTHORIZATION, TokensDePrueba.bearer(turistaId, "TURISTA")));
 * }</pre>
 */
public final class TokensDePrueba {

    public static final String EMISOR = "samarian-tour-nucleo";
    public static final String EMISOR_RECOMENDACION = "samarian-tour-recomendacion";

    private static final KeyPair CLAVES = generar();
    private static final KeyPair OTRAS_CLAVES = generar();
    private static final KeyPair CLAVES_RECOMENDACION = generar();

    /** La clave pública, en X.509 y Base64, como la de {@code SEGURIDAD_JWT_CLAVE_PUBLICA}. */
    public static final String CLAVE_PUBLICA = base64(CLAVES.getPublic().getEncoded());
    /** La clave privada, en PKCS#8 y Base64, como la de {@code NUCLEO_JWT_CLAVE_PRIVADA}. */
    public static final String CLAVE_PRIVADA = base64(CLAVES.getPrivate().getEncoded());
    /** La pareja con la que firma la recomendación sus tokens de servicio (DN-38). */
    public static final String CLAVE_PUBLICA_RECOMENDACION = base64(CLAVES_RECOMENDACION.getPublic().getEncoded());
    public static final String CLAVE_PRIVADA_RECOMENDACION = base64(CLAVES_RECOMENDACION.getPrivate().getEncoded());

    private TokensDePrueba() {
    }

    /** {@code Bearer <token>} de un usuario con ese rol, válido por 15 minutos. */
    public static String bearer(Object usuarioId, String rol) {
        return "Bearer " + firmar(datos(usuarioId, rol, null).build(), CLAVES);
    }

    /** {@code Bearer <token>} de un prestador; {@code tipo} null si todavía no registró su RNT. */
    public static String bearerPrestador(Object usuarioId, String tipo) {
        return "Bearer " + firmar(datos(usuarioId, ReclamosToken.ROL_PRESTADOR, tipo).build(), CLAVES);
    }

    /** Un token que venció hace 5 minutos, más allá de la tolerancia de 60 s. */
    public static String bearerVencido(Object usuarioId, String rol) {
        Instant hace20 = Instant.now().minus(Duration.ofMinutes(20));
        return "Bearer " + firmar(datos(usuarioId, rol, null).issuedAt(hace20)
                .expiresAt(hace20.plus(Duration.ofMinutes(15))).build(), CLAVES);
    }

    /** Un token bien formado pero firmado con otra clave: lo que haría quien intenta falsificarlo. */
    public static String bearerConOtraClave(Object usuarioId, String rol) {
        return "Bearer " + firmar(datos(usuarioId, rol, null).build(), OTRAS_CLAVES);
    }

    /** Un token firmado con la clave correcta y ajustado a gusto, para probar cada regla de validación. */
    public static String bearerAjustado(Object usuarioId, String rol, Consumer<JwtClaimsSet.Builder> ajuste) {
        JwtClaimsSet.Builder datos = datos(usuarioId, rol, null);
        ajuste.accept(datos);
        return "Bearer " + firmar(datos.build(), CLAVES);
    }

    /**
     * {@code Bearer <token>} de servicio, como lo firma {@link EmisorTokenServicio}: {@code nucleo} con la clave del
     * núcleo y {@code recomendacion} con la suya.
     */
    public static String bearerServicio(String servicio, String audiencia) {
        return "Bearer " + firmar(datosServicio(servicio, audiencia).build(), clavesDe(servicio));
    }

    /** Un token de servicio con la forma correcta pero firmado con otra clave. */
    public static String bearerServicioConOtraClave(String servicio, String audiencia) {
        return "Bearer " + firmar(datosServicio(servicio, audiencia).build(), OTRAS_CLAVES);
    }

    private static JwtClaimsSet.Builder datosServicio(String servicio, String audiencia) {
        Instant ahora = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(ServiciosInternos.RECOMENDACION.equals(servicio) ? EMISOR_RECOMENDACION : EMISOR)
                .subject(servicio)
                .audience(List.of(audiencia))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(EmisorTokenServicio.VIGENCIA));
    }

    private static KeyPair clavesDe(String servicio) {
        return ServiciosInternos.RECOMENDACION.equals(servicio) ? CLAVES_RECOMENDACION : CLAVES;
    }

    private static JwtClaimsSet.Builder datos(Object usuarioId, String rol, String tipo) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder datos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuarioId.toString())
                .claim(ReclamosToken.ROL, rol)
                .claim(ReclamosToken.SESION, "00000000-0000-0000-0000-00000000000a")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofMinutes(15)));
        if (tipo != null) {
            datos.claim(ReclamosToken.TIPO_PRESTADOR, tipo);
        }
        return datos;
    }

    private static String firmar(JwtClaimsSet datos, KeyPair claves) {
        RSAKey clave = new RSAKey.Builder((RSAPublicKey) claves.getPublic())
                .privateKey((RSAPrivateKey) claves.getPrivate())
                .build();
        NimbusJwtEncoder firmador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return firmador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
    }

    private static KeyPair generar() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64(byte[] der) {
        return Base64.getEncoder().encodeToString(der);
    }
}
