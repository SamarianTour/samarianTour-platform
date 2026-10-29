package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** El administrador configura el porcentaje de comisión de la plataforma (RF-82, RF-83, DN-23). */
public interface ActualizarComisionUseCase {

    /** El historial, del cambio más reciente al más antiguo: el primero es el vigente. */
    List<ConfiguracionComision> historial();

    /**
     * Rige para los pagos que se confirmen desde ahora; los ya confirmados conservan el suyo (RF-83).
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.DatoInvalidoException
     */
    ConfiguracionComision actualizar(BigDecimal porcentaje, UUID administradorId);
}
