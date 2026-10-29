package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ActualizarComisionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ComisionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * La comisión de la plataforma (RF-82, RF-83, DN-23): cada cambio agrega una configuración vigente desde ahora. Los
 * pagos ya confirmados guardaron el porcentaje que se les aplicó, así que no se tocan.
 */
public class ActualizarComisionService implements ActualizarComisionUseCase {

    private final ComisionRepositoryPort comisiones;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public ActualizarComisionService(ComisionRepositoryPort comisiones, TransaccionPort transaccion, Clock reloj) {
        this.comisiones = comisiones;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public List<ConfiguracionComision> historial() {
        return comisiones.historial();
    }

    @Override
    public ConfiguracionComision actualizar(BigDecimal porcentaje, UUID administradorId) {
        ConfiguracionComision nueva = new ConfiguracionComision(porcentaje, reloj.instant(), administradorId);
        return transaccion.ejecutar(() -> {
            comisiones.agregar(nueva);
            return nueva;
        });
    }
}
