package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ComisionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** El historial de la comisión en {@code configuracion_comision} (DN-23). */
@Component
public class ComisionJpaAdapter implements ComisionRepositoryPort {

    private final ConfiguracionComisionJpaRepository comisiones;

    ComisionJpaAdapter(ConfiguracionComisionJpaRepository comisiones) {
        this.comisiones = comisiones;
    }

    @Override
    public List<ConfiguracionComision> historial() {
        return comisiones.findAllByOrderByVigenteDesdeDescIdDesc().stream()
                .map(fila -> new ConfiguracionComision(fila.porcentaje(), fila.vigenteDesde(), fila.administradorId()))
                .toList();
    }

    @Override
    public void agregar(ConfiguracionComision configuracion) {
        comisiones.saveAndFlush(new ConfiguracionComisionEntity(UUID.randomUUID(), configuracion.porcentaje(),
                configuracion.vigenteDesde(), configuracion.administradorId()));
    }
}
