package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.ObservadorCuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.AvisoCuentaCobroPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Avisa del cambio de cuenta a cada {@link ObservadorCuentaCobro} que Spring encuentre (DN-44): hoy, el de pagos.
 * Prestadores no importa nada de los módulos que lo escuchan.
 */
@Component
public class AvisoCuentaCobroAdapter implements AvisoCuentaCobroPort {

    private final List<ObservadorCuentaCobro> observadores;

    public AvisoCuentaCobroAdapter(List<ObservadorCuentaCobro> observadores) {
        this.observadores = observadores;
    }

    @Override
    public void cuentaCambiada(UUID prestadorId) {
        observadores.forEach(observador -> observador.cuentaCambiada(prestadorId));
    }
}
