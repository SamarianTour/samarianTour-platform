package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ReactivarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.ObservadorCuentaCobro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cuando el prestador cambia su cuenta de cobro, sus dispersiones fallidas vuelven a quedar pendientes (RF-54,
 * DN-44). Pagos escucha a prestadores sin que prestadores dependa de pagos.
 */
@Component
public class DispersionesCuentaCobroAdapter implements ObservadorCuentaCobro {

    private static final Logger LOG = LoggerFactory.getLogger(DispersionesCuentaCobroAdapter.class);

    private final ReactivarDispersionesUseCase reactivar;

    public DispersionesCuentaCobroAdapter(ReactivarDispersionesUseCase reactivar) {
        this.reactivar = reactivar;
    }

    @Override
    public void cuentaCambiada(UUID prestadorId) {
        int reactivadas = reactivar.reactivar(prestadorId);
        if (reactivadas > 0) {
            LOG.info("dispersiones: {} fallidas del prestador {} vuelven a pendiente con la cuenta nueva",
                    reactivadas, prestadorId);
        }
    }
}
