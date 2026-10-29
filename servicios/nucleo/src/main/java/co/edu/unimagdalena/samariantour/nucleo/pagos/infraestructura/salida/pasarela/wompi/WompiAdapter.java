package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.wompi;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoVerificado;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OrdenDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config.PropiedadesPagos;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.DispersionSimulada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.FormatoWompi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Wompi, el adaptador objetivo (SRS, glosario; QAS-06, DN-18). Se activa con {@code nucleo.pagos.pasarela=wompi} y
 * las llaves del sandbox o de producción en {@code infra/.env}; el núcleo no arranca si faltan.
 * <ul>
 *   <li><b>Sesión de cobro</b>: el Web Checkout ({@code https://checkout.wompi.co/p/}) con la llave pública, el monto
 *   en centavos, la moneda, la referencia, la firma de integridad y la URL de redirección. El turista paga en Wompi con
 *   tarjeta, PSE, Nequi o Daviplata y el backend nunca ve sus datos (RF-47, RF-48). La misma información sirve para el
 *   widget.</li>
 *   <li><b>Eventos</b>: el checksum de Wompi con el secreto de eventos (RF-50).</li>
 *   <li><b>Dispersión</b>: "Pagos a terceros" de Wompi es otra API, con credenciales propias, que se habilita con una
 *   activación aparte de la cuenta del comercio (validación del representante legal, de 2 a 5 días hábiles). Mientras
 *   no esté activa, se delega a la dispersión simulada (DN-18).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "nucleo.pagos.pasarela", havingValue = "wompi")
public class WompiAdapter implements PaymentGatewayPort {

    static final String PASARELA = "WOMPI";

    private final PropiedadesPagos.Wompi wompi;
    private final String urlRedireccion;
    private final DispersionSimulada dispersion;
    private final ObjectMapper json;

    public WompiAdapter(PropiedadesPagos propiedades, ObjectMapper json) {
        this.wompi = propiedades.wompi();
        if (wompi.llavePublica().isBlank() || wompi.secretoIntegridad().isBlank() || wompi.secretoEventos().isBlank()) {
            throw new IllegalStateException("La pasarela es wompi pero faltan WOMPI_LLAVE_PUBLICA, "
                    + "WOMPI_SECRETO_INTEGRIDAD o WOMPI_SECRETO_EVENTOS en infra/.env");
        }
        this.urlRedireccion = propiedades.urlRedireccion();
        this.dispersion = new DispersionSimulada(propiedades.simulada().cuentasQueFallan());
        this.json = json;
    }

    @Override
    public SesionCobro crearSesion(IntentoPago intento) {
        String firma = FormatoWompi.firmaIntegridad(intento.referencia(), intento.montoCentavos(), IntentoPago.MONEDA,
                wompi.secretoIntegridad());
        String redireccion = urlRedireccion.replace("{id}", intento.solicitudId().toString());
        String url = UriComponentsBuilder.fromUriString(wompi.urlCheckout())
                .queryParam("public-key", wompi.llavePublica())
                .queryParam("currency", IntentoPago.MONEDA)
                .queryParam("amount-in-cents", intento.montoCentavos())
                .queryParam("reference", intento.referencia())
                .queryParam("signature:integrity", firma)
                .queryParam("redirect-url", redireccion)
                .encode(StandardCharsets.UTF_8)
                .build().toUriString();
        return new SesionCobro(PASARELA, url, wompi.llavePublica(), firma, redireccion);
    }

    @Override
    public Optional<EventoVerificado> verificarEvento(String cuerpo, String firmaCabecera) {
        return FormatoWompi.verificar(cuerpo, firmaCabecera, wompi.secretoEventos(), json);
    }

    @Override
    public ResultadoDispersion dispersar(OrdenDispersion orden) {
        return dispersion.dispersar(orden);
    }
}
