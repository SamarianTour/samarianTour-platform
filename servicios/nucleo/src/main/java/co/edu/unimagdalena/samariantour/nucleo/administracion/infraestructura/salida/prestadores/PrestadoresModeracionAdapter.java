package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.PrestadoresModeracionPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadorConsultado;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadoresApiInterna;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** La habilitación de los prestadores, por su API (DN-35). */
@Component
public class PrestadoresModeracionAdapter implements PrestadoresModeracionPort {

    private final PrestadoresApiInterna prestadores;

    public PrestadoresModeracionAdapter(PrestadoresApiInterna prestadores) {
        this.prestadores = prestadores;
    }

    @Override
    public boolean estaHabilitado(UUID prestadorId) {
        return prestadores.consultar(prestadorId).map(PrestadorConsultado::habilitado).orElse(false);
    }
}
