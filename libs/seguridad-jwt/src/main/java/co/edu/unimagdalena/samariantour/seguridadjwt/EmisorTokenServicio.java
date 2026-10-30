package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateCrtKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Firma el token con el que un servicio llama a las rutas internas de otro (DN-38). Lo firma con su propia clave
 * privada, y el que recibe lo valida con la pública ({@link DecodificadorTokenServicio}). El token lleva:
 * <ul>
 *   <li>{@code iss}: el emisor del servicio que llama, y {@code sub}: su nombre ({@code nucleo} o
 *       {@code recomendacion});</li>
 *   <li>{@code aud}: el servicio que lo recibe ({@code catalogo}), así que no sirve contra otro;</li>
 *   <li>{@code exp}: 5 minutos. Se reutiliza hasta que le falta un minuto y entonces se firma otro.</li>
 * </ul>
 * No lleva {@code rol}: ninguna ruta de usuario lo acepta, y ninguna ruta interna acepta el token de un usuario.
 */
public final class EmisorTokenServicio {

    public static final Duration VIGENCIA = Duration.ofMinutes(5);
    private static final Duration MARGEN = Duration.ofMinutes(1);

    private final String servicio;
    private final String emisor;
    private final JwtEncoder firmador;
    private final String idClave;
    private final Clock reloj;
    private final Map<String, Firmado> vigentes = new ConcurrentHashMap<>();

    private record Firmado(String valor, Instant expira) {
    }

    /**
     * @param servicio el nombre del servicio que llama, que va en {@code sub}
     * @param emisor   su emisor, que va en {@code iss}
     */
    public EmisorTokenServicio(String servicio, String emisor, RSAPrivateCrtKey clavePrivada, Clock reloj) {
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        this.emisor = Objects.requireNonNull(emisor, "emisor");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        RSAKey clave;
        try {
            clave = new RSAKey.Builder(ClavesRsa.publicaDe(clavePrivada)).privateKey(clavePrivada)
                    .keyIDFromThumbprint().build();
        } catch (JOSEException e) {
            throw new IllegalArgumentException("La clave del servicio no es una clave RSA válida", e);
        }
        this.idClave = clave.getKeyID();
        this.firmador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
    }

    /** Un token vigente para llamar a {@code audiencia}. */
    public String token(String audiencia) {
        Instant ahora = reloj.instant();
        return vigentes.compute(audiencia, (destino, actual) ->
                actual != null && actual.expira().minus(MARGEN).isAfter(ahora) ? actual : firmar(destino, ahora))
                .valor();
    }

    /**
     * Agrega el token a las peticiones hacia {@code /internal/**} de {@code audiencia}; las demás, como las rutas
     * públicas del catálogo que también consulta la recomendación, salen sin él.
     */
    public ClientHttpRequestInterceptor interceptor(String audiencia) {
        return (peticion, cuerpo, ejecucion) -> {
            if (peticion.getURI().getRawPath().contains("/internal/")) {
                peticion.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token(audiencia));
            }
            return ejecucion.execute(peticion, cuerpo);
        };
    }

    private Firmado firmar(String audiencia, Instant ahora) {
        Instant expira = ahora.plus(VIGENCIA);
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(servicio)
                .audience(List.of(audiencia))
                .issuedAt(ahora)
                .expiresAt(expira)
                .build();
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(idClave).build();
        return new Firmado(firmador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue(), expira);
    }
}
