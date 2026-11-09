package co.edu.unimagdalena.samariantour.catalogo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La configuración de Nginx del servidor de AWS ({@code infra/nginx/nginx-aws.conf}, DN-46) con un certificado
 * autofirmado en el lugar del de Let's Encrypt: el 80 solo sirve el desafío y redirige, y el 443 tiene las mismas
 * rutas que en local, con HSTS y HTTP/2. Las rutas bloqueadas, una por una, las prueba {@link NginxRutasInternasIT}.
 */
class NginxAwsIT {

    private static final String ECO = """
            events {}
            http {
                server {
                    listen 8080;
                    location / {
                        default_type text/plain;
                        return 200 "eco $request_uri";
                    }
                }
            }
            """;
    private static final String DESAFIO = "archivo-del-desafio-de-prueba";
    private static final String CLAVE_ALMACEN = "solo-para-la-prueba";
    private static final String HSTS = "max-age=31536000";

    @TempDir
    static Path carpeta;

    private static Network red;
    private static GenericContainer<?> servicios;
    private static GenericContainer<?> nginx;
    private static HttpClient http;
    private static HttpClient https;

    @BeforeAll
    static void iniciar() throws Exception {
        KeyStore almacen = certificadoAutofirmado();
        Certificate certificado = almacen.getCertificate("nginx");
        byte[] clave = almacen.getKey("nginx", CLAVE_ALMACEN.toCharArray()).getEncoded();

        red = Network.newNetwork();
        servicios = new GenericContainer<>("nginx:1.27-alpine")
                .withNetwork(red)
                .withNetworkAliases("catalogo", "nucleo", "recomendacion")
                .withCopyToContainer(Transferable.of(ECO), "/etc/nginx/nginx.conf");
        nginx = new GenericContainer<>("nginx:1.27-alpine")
                .withNetwork(red)
                .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../../infra/nginx/nginx-aws.conf")),
                        "/etc/nginx/nginx.conf")
                .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../../infra/nginx/rutas.conf")),
                        "/etc/nginx/rutas.conf")
                .withCopyToContainer(Transferable.of(pem("CERTIFICATE", certificado.getEncoded())),
                        "/etc/letsencrypt/live/samarian/fullchain.pem")
                .withCopyToContainer(Transferable.of(pem("PRIVATE KEY", clave)),
                        "/etc/letsencrypt/live/samarian/privkey.pem")
                .withCopyToContainer(Transferable.of(DESAFIO), "/var/www/certbot/.well-known/acme-challenge/prueba")
                .withExposedPorts(80, 443)
                .dependsOn(servicios)
                .waitingFor(Wait.forHttp("/.well-known/acme-challenge/prueba").forPort(80));
        nginx.start();

        KeyStore confianza = KeyStore.getInstance(KeyStore.getDefaultType());
        confianza.load(null, null);
        confianza.setCertificateEntry("nginx", certificado);
        TrustManagerFactory confiables = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        confiables.init(confianza);
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(null, confiables.getTrustManagers(), null);
        http = HttpClient.newHttpClient();
        https = HttpClient.newBuilder().sslContext(tls).build();
    }

    @AfterAll
    static void detener() {
        nginx.stop();
        servicios.stop();
        red.close();
    }

    @Test
    @DisplayName("por HTTP solo sirve el desafío de Let's Encrypt; lo demás lo redirige a HTTPS con la misma ruta")
    void http() throws Exception {
        HttpResponse<String> desafio = http.send(peticion("http", 80, "/.well-known/acme-challenge/prueba"),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> api = http.send(peticion("http", 80, "/api/catalogo/zonas?texto=playa"),
                HttpResponse.BodyHandlers.ofString());

        assertThat(desafio.statusCode()).isEqualTo(200);
        assertThat(desafio.body()).isEqualTo(DESAFIO);
        assertThat(api.statusCode()).isEqualTo(301);
        assertThat(api.headers().firstValue("Location"))
                .contains("https://" + nginx.getHost() + "/api/catalogo/zonas?texto=playa");
        assertThat(api.body()).doesNotContain("eco");
    }

    @Test
    @DisplayName("por HTTPS, con HTTP/2 y HSTS, las rutas públicas llegan al servicio y las internas no")
    void https() throws Exception {
        HttpResponse<String> publica = https.send(peticion("https", 443, "/api/catalogo/zonas"),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> interna = https.send(peticion("https", 443, "/api/nucleo/internal/exposiciones"),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> conPuntoYComa = https.send(peticion("https", 443, "/api/catalogo/internal;x/avisos"),
                HttpResponse.BodyHandlers.ofString());

        assertThat(publica.statusCode()).isEqualTo(200);
        assertThat(publica.body()).isEqualTo("eco /api/catalogo/zonas");
        assertThat(publica.version()).isEqualTo(HttpClient.Version.HTTP_2);
        assertThat(publica.headers().firstValue("Strict-Transport-Security")).contains(HSTS);
        assertThat(interna.statusCode()).isEqualTo(404);
        assertThat(interna.headers().firstValue("Strict-Transport-Security")).contains(HSTS);
        assertThat(conPuntoYComa.statusCode()).isEqualTo(400);
        assertThat(List.of(interna.body(), conPuntoYComa.body())).noneMatch(cuerpo -> cuerpo.contains("eco"));
    }

    private static HttpRequest peticion(String esquema, int puerto, String ruta) {
        URI uri = URI.create(esquema + "://" + nginx.getHost() + ":" + nginx.getMappedPort(puerto) + ruta);
        return HttpRequest.newBuilder(uri).GET().build();
    }

    /** Un certificado de 2 días para localhost, hecho con el keytool del JDK, en un almacén PKCS12. */
    private static KeyStore certificadoAutofirmado() throws Exception {
        Path archivo = carpeta.resolve("nginx.p12");
        Process keytool = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "nginx", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-storetype", "PKCS12",
                "-keystore", archivo.toString(), "-storepass", CLAVE_ALMACEN, "-keypass", CLAVE_ALMACEN)
                .redirectErrorStream(true)
                .start();
        String salida = new String(keytool.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(keytool.waitFor()).as(salida).isZero();
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        try (InputStream entrada = Files.newInputStream(archivo)) {
            almacen.load(entrada, CLAVE_ALMACEN.toCharArray());
        }
        return almacen;
    }

    private static String pem(String tipo, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + tipo + "-----\n" + base64 + "\n-----END " + tipo + "-----\n";
    }
}
