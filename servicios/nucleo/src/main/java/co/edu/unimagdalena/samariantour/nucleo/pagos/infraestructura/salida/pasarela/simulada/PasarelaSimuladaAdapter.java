package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.simulada;

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

import java.util.Optional;

/**
 * La pasarela simulada del prototipo (SRS, glosario "Wompi"; QAS-06, DN-18), la que corre por defecto:
 * <ul>
 *   <li>la sesión de cobro apunta a la página de pago simulada del propio núcleo, con el monto y la referencia;</li>
 *   <li>los eventos tienen la misma forma que los de Wompi y se verifican con el mismo checksum, con un secreto de
 *   demo, así la demo recorre el mismo camino que un pago real;</li>
 *   <li>la dispersión responde éxito, salvo a las cuentas configuradas para fallar.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "nucleo.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
public class PasarelaSimuladaAdapter implements PaymentGatewayPort {

    static final String PASARELA = "SIMULADA";

    private final PropiedadesPagos propiedades;
    private final DispersionSimulada dispersion;
    private final ObjectMapper json;

    public PasarelaSimuladaAdapter(PropiedadesPagos propiedades, ObjectMapper json) {
        this.propiedades = propiedades;
        this.dispersion = new DispersionSimulada(propiedades.simulada().cuentasQueFallan());
        this.json = json;
    }

    @Override
    public SesionCobro crearSesion(IntentoPago intento) {
        String url = propiedades.simulada().urlPagina() + PaginaPasarelaSimulada.RUTA + "/" + intento.referencia()
                + "?monto=" + intento.montoCentavos() + "&solicitud=" + intento.solicitudId();
        return new SesionCobro(PASARELA, url, null, null, redireccion(propiedades, intento));
    }

    @Override
    public Optional<EventoVerificado> verificarEvento(String cuerpo, String firmaCabecera) {
        return FormatoWompi.verificar(cuerpo, firmaCabecera, propiedades.simulada().secretoEventos(), json);
    }

    @Override
    public ResultadoDispersion dispersar(OrdenDispersion orden) {
        return dispersion.dispersar(orden);
    }

    static String redireccion(PropiedadesPagos propiedades, IntentoPago intento) {
        return propiedades.urlRedireccion().replace("{id}", intento.solicitudId().toString());
    }
}
