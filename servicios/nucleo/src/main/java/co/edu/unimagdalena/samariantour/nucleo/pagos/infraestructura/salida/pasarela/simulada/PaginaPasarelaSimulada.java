package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.simulada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config.PropiedadesPagos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * La página de pago de la pasarela simulada (DN-18), solo con el perfil demo: hace el papel del checkout de Wompi, por
 * eso vive con su adaptador y no es parte de la API del núcleo. Muestra el monto y la referencia, y dos botones,
 * "Aprobar" y "Rechazar"; al pulsar uno, la pasarela simulada envía el evento al webhook unos segundos después. La
 * página nunca cambia la solicitud por su cuenta (RF-51).
 */
@Controller
@Profile("demo")
@ConditionalOnProperty(name = "nucleo.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
@RequestMapping(PaginaPasarelaSimulada.RUTA)
public class PaginaPasarelaSimulada {

    static final String RUTA = "/pasarela-simulada";
    private static final Pattern REFERENCIA = Pattern.compile("ST-[0-9a-f-]{36}");

    private final EmisorEventosSimulados emisor;
    private final PropiedadesPagos propiedades;

    public PaginaPasarelaSimulada(EmisorEventosSimulados emisor, PropiedadesPagos propiedades) {
        this.emisor = emisor;
        this.propiedades = propiedades;
    }

    @GetMapping(value = "/{referencia}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> pagina(@PathVariable String referencia, @RequestParam long monto,
                                         @RequestParam UUID solicitud) {
        if (!REFERENCIA.matcher(referencia).matches() || monto <= 0) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(html("Pago no válido",
                    "<p>La referencia o el monto no son válidos.</p>"));
        }
        String ref = HtmlUtils.htmlEscape(referencia);
        String campos = "<input type=\"hidden\" name=\"monto\" value=\"" + monto + "\">"
                + "<input type=\"hidden\" name=\"solicitud\" value=\"" + solicitud + "\">";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html("Pasarela simulada",
                "<p>Este es el checkout de la <strong>pasarela simulada</strong> de la demo. No pide ni recibe datos de"
                        + " tarjeta ni de cuenta (RF-47).</p>"
                        + "<dl><dt>Monto</dt><dd>" + pesos(monto) + " COP</dd>"
                        + "<dt>Referencia</dt><dd><code>" + ref + "</code></dd></dl>"
                        + "<form method=\"post\" action=\"" + ref + "/aprobar\">" + campos
                        + "<button type=\"submit\">Aprobar</button></form>"
                        + "<form method=\"post\" action=\"" + ref + "/rechazar\">" + campos
                        + "<button type=\"submit\">Rechazar</button></form>"));
    }

    @PostMapping(value = "/{referencia}/aprobar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> aprobar(@PathVariable String referencia, @RequestParam long monto,
                                          @RequestParam UUID solicitud) {
        return resolver(referencia, monto, solicitud, "APPROVED", "aprobado");
    }

    @PostMapping(value = "/{referencia}/rechazar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> rechazar(@PathVariable String referencia, @RequestParam long monto,
                                           @RequestParam UUID solicitud) {
        return resolver(referencia, monto, solicitud, "DECLINED", "rechazado");
    }

    private ResponseEntity<String> resolver(String referencia, long monto, UUID solicitud, String estado,
                                            String texto) {
        if (!REFERENCIA.matcher(referencia).matches() || monto <= 0) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(html("Pago no válido",
                    "<p>La referencia o el monto no son válidos.</p>"));
        }
        String transaccion = emisor.programar(referencia, monto, estado);
        String volver = propiedades.urlRedireccion().replace("{id}", solicitud.toString()) + "?id=" + transaccion;
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html("Pago " + texto,
                "<p>La pasarela simulada marcó el pago como <strong>" + texto + "</strong>. El resultado llega a la "
                        + "plataforma por el webhook en unos segundos.</p>"
                        + "<p>Transacción: <code>" + HtmlUtils.htmlEscape(transaccion) + "</code></p>"
                        + "<p><a href=\"" + HtmlUtils.htmlEscape(volver) + "\">Volver a la solicitud</a></p>"));
    }

    private static String pesos(long centavos) {
        return "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(centavos / 100);
    }

    private static String html(String titulo, String cuerpo) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>" + titulo
                + "</title><style>body{font-family:system-ui,sans-serif;max-width:32rem;margin:2rem auto;"
                + "padding:0 1rem}"
                + "form{display:inline-block;margin-right:.5rem}button{font-size:1rem;padding:.5rem 1.25rem}"
                + "dt{font-weight:600}</style></head><body><h1>" + titulo + "</h1>" + cuerpo + "</body></html>";
    }
}
