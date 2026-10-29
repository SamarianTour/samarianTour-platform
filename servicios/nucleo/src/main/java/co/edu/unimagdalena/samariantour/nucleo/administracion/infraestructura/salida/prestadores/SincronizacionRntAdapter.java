package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.ResultadoSincronizacionRnt;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.SincronizacionRntPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadoresApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.SincronizacionRnt;
import org.springframework.stereotype.Component;

/** La sincronización con el RNT, por la API de los prestadores (DN-36). */
@Component
public class SincronizacionRntAdapter implements SincronizacionRntPort {

    private final PrestadoresApiInterna prestadores;

    public SincronizacionRntAdapter(PrestadoresApiInterna prestadores) {
        this.prestadores = prestadores;
    }

    @Override
    public ResultadoSincronizacionRnt sincronizar() {
        SincronizacionRnt resultado = prestadores.sincronizarRnt();
        return new ResultadoSincronizacionRnt(resultado.revisados(), resultado.retirados(), resultado.restituidos(),
                resultado.sinRespuesta(), resultado.interrumpida());
    }
}
