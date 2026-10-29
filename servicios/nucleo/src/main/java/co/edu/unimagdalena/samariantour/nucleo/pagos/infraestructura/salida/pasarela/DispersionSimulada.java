package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OrdenDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoDispersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * La dispersión simulada (DN-24): responde éxito, salvo a las cuentas configuradas para fallar, que sirven para mostrar
 * los reintentos y el aviso al prestador. La usa también el adaptador de Wompi, porque su producto de dispersión
 * ("Pagos a terceros") exige una activación aparte de la cuenta del comercio (DN-18). El log solo lleva los últimos 4
 * dígitos de la cuenta (RF-75).
 */
public class DispersionSimulada {

    private static final Logger LOG = LoggerFactory.getLogger(DispersionSimulada.class);

    private final List<String> cuentasQueFallan;

    public DispersionSimulada(List<String> cuentasQueFallan) {
        this.cuentasQueFallan = List.copyOf(cuentasQueFallan);
    }

    public ResultadoDispersion dispersar(OrdenDispersion orden) {
        if (cuentasQueFallan.contains(orden.cuenta().numero())) {
            LOG.info("[Dispersión simulada] rechazada: dispersion={} cuenta={} monto={}", orden.dispersionId(),
                    orden.cuenta().enmascarada(), orden.montoNeto());
            return ResultadoDispersion.fallo("La entidad rechazó la cuenta " + orden.cuenta().enmascarada()
                    + " (simulado)");
        }
        LOG.info("[Dispersión simulada] enviada: dispersion={} cuenta={} monto={}", orden.dispersionId(),
                orden.cuenta().enmascarada(), orden.montoNeto());
        return ResultadoDispersion.exito("DSP-SIM-" + orden.dispersionId());
    }
}
