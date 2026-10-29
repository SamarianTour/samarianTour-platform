package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.rnt;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.RegistroRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.RntVerificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * El RNT real (RF-62, RF-63, RF-76, DN-33): el conjunto "Registro Nacional de Turismo" del Ministerio en
 * datos.gov.co, por su API SODA ({@code ?codigo_rnt=<número>}). Se elige con {@code nucleo.rnt.fuente=soda}.
 * <ul>
 *   <li>El conjunto solo publica los registros activos: si el número no aparece, no se encuentra.</li>
 *   <li>Espera como máximo 3 s (RNF-05). Si no responde o falla, el resultado es {@code NoDisponible}, nunca un
 *   error: el prestador queda en Verificación pendiente (QAS-03).</li>
 *   <li>Sin token de aplicación, Socrata limita las consultas por IP; con {@code nucleo.rnt.soda.token-aplicacion}
 *   el límite sube.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "nucleo.rnt.fuente", havingValue = "soda")
public class RntSodaAdapter implements RntVerificationPort {

    static final Duration ESPERA = Duration.ofSeconds(3);
    private static final Logger LOG = LoggerFactory.getLogger(RntSodaAdapter.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> FILAS =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient cliente;

    public RntSodaAdapter(@Value("${nucleo.rnt.soda.url}") String url,
                          @Value("${nucleo.rnt.soda.token-aplicacion:}") String tokenAplicacion) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(ESPERA).build());
        fabrica.setReadTimeout(ESPERA);
        RestClient.Builder constructor = RestClient.builder().baseUrl(url).requestFactory(fabrica);
        if (!tokenAplicacion.isBlank()) {
            constructor.defaultHeader("X-App-Token", tokenAplicacion);
        }
        this.cliente = constructor.build();
    }

    @Override
    public ConsultaRnt consultar(String numeroRnt) {
        try {
            List<Map<String, Object>> filas = cliente.get()
                    .uri(uri -> uri.queryParam("codigo_rnt", numeroRnt).queryParam("$limit", 1).build())
                    .retrieve()
                    .body(FILAS);
            if (filas == null || filas.isEmpty()) {
                return new ConsultaRnt.NoEncontrado();
            }
            return new ConsultaRnt.Encontrado(aRegistro(numeroRnt, filas.getFirst()));
        } catch (RestClientException | IllegalArgumentException e) {
            LOG.warn("El RNT no respondió para el número {}: {}", numeroRnt, e.getMessage());
            return new ConsultaRnt.NoDisponible(e.getClass().getSimpleName());
        }
    }

    static RegistroRnt aRegistro(String numeroRnt, Map<String, Object> fila) {
        return new RegistroRnt(numeroRnt, texto(fila, "razon_social_establecimiento"), texto(fila, "municipio"),
                texto(fila, "categoria"), texto(fila, "sub_categoria"),
                "NO COMERCIANTE".equalsIgnoreCase(texto(fila, "tipo_prestador")), entero(fila.get("num_emp1")));
    }

    private static String texto(Map<String, Object> fila, String campo) {
        Object valor = fila.get(campo);
        return valor == null ? "" : valor.toString().strip();
    }

    private static Integer entero(Object valor) {
        try {
            return valor == null ? null : Integer.valueOf(valor.toString().strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
