package co.edu.unimagdalena.samariantour.nucleo.pagos.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lo que el módulo de administración usa de pagos (DN-23): la comisión de la plataforma (RF-82, RF-83) y los pagos
 * tardíos para el reembolso manual (RF-50). Java puro, con sus propios tipos, como {@code solicitudes.api}.
 */
public interface PagosAdministracionApi {

    /** Del cambio más reciente al más antiguo: el primero es el vigente. */
    List<ComisionConfigurada> historialComision();

    /**
     * Rige para los pagos que se confirmen desde ahora; los ya confirmados conservan el suyo (RF-83).
     *
     * @throws ComisionInvalidaException si el porcentaje no es válido
     */
    ComisionConfigurada actualizarComision(BigDecimal porcentaje, UUID administradorId);

    /** Del más reciente al más antiguo. */
    List<PagoTardioRegistrado> pagosTardios();
}
