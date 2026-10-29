package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * La configuración de pagos ({@code nucleo.pagos}, DN-18). Los secretos de Wompi llegan por variables de entorno desde
 * {@code infra/.env}, que no se versiona: nunca se escriben aquí ni en {@code application.yml}.
 *
 * @param pasarela       {@code simulada} (por defecto, la del prototipo) o {@code wompi}
 * @param urlRedireccion a dónde vuelve el turista después de pagar, con {@code {id}} en lugar de la solicitud; no es
 *                       prueba del pago (RF-51)
 */
@ConfigurationProperties("nucleo.pagos")
public record PropiedadesPagos(@DefaultValue("simulada") String pasarela,
                               @DefaultValue("http://localhost:8080/turista/solicitudes/{id}") String urlRedireccion,
                               @DefaultValue Costo costoTransaccional, @DefaultValue Dispersion dispersion,
                               @DefaultValue Simulada simulada, @DefaultValue Wompi wompi) {

    /**
     * El costo transaccional de la pasarela [SUPUESTO] (DN-23): por defecto, la tarifa publicada del Plan Avanzado de
     * Wompi (2,65 % + $700 + IVA del 19 %).
     */
    public record Costo(@DefaultValue("2.65") BigDecimal porcentaje, @DefaultValue("700") long fijo,
                        @DefaultValue("19") BigDecimal iva) {
    }

    /** @param esperaBase antes del primer reintento de una dispersión; se duplica en cada uno [SUPUESTO] (DN-24) */
    public record Dispersion(@DefaultValue("1m") Duration esperaBase) {
    }

    /**
     * La pasarela simulada (DN-18).
     *
     * @param urlPagina      la URL pública del núcleo donde vive la página de pago simulada
     * @param urlWebhook     a dónde envía sus eventos: el webhook del mismo núcleo
     * @param secretoEventos con el que firma sus eventos, como Wompi; es de demo, no de Wompi
     * @param demoraEvento   cuánto espera antes de enviar el evento, como una pasarela real
     * @param cuentasQueFallan los números de cuenta a los que la dispersión simulada siempre falla, para mostrar los
     *                       reintentos y el aviso al prestador
     */
    public record Simulada(@DefaultValue("http://localhost:8080/api/nucleo") String urlPagina,
                           @DefaultValue("http://localhost:8080/api/nucleo/pagos/eventos") String urlWebhook,
                           @DefaultValue("secreto-de-eventos-solo-para-la-demo") String secretoEventos,
                           @DefaultValue("3s") Duration demoraEvento,
                           @DefaultValue List<String> cuentasQueFallan) {

        public Simulada {
            cuentasQueFallan = cuentasQueFallan == null ? List.of() : List.copyOf(cuentasQueFallan);
        }
    }

    /**
     * Wompi (DN-18). Las llaves y secretos son los del sandbox o de producción según la cuenta: la llave pública
     * ({@code pub_test_...}), el secreto de integridad ({@code test_integrity_...}) y el de eventos
     * ({@code test_events_...}).
     */
    public record Wompi(@DefaultValue("") String llavePublica, @DefaultValue("") String secretoIntegridad,
                        @DefaultValue("") String secretoEventos,
                        @DefaultValue("https://checkout.wompi.co/p/") String urlCheckout) {

        /** Sin mostrar los secretos, por si el registro llega a un log. */
        @Override
        public String toString() {
            return "Wompi[llavePublica=" + llavePublica + ", urlCheckout=" + urlCheckout + "]";
        }
    }
}
