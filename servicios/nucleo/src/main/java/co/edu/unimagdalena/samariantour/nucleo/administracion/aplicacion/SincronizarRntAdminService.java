package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.ResultadoSincronizacionRnt;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.SincronizarRntAdminUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.SincronizacionRntPort;

/**
 * Dispara la sincronización con el RNT (RF-76, DN-36). El trabajo lo hacen los prestadores: comparan cada uno con el
 * RNT, avisan al catálogo (RF-77) y al prestador retirado (RF-78).
 */
public class SincronizarRntAdminService implements SincronizarRntAdminUseCase {

    private final SincronizacionRntPort prestadores;

    public SincronizarRntAdminService(SincronizacionRntPort prestadores) {
        this.prestadores = prestadores;
    }

    @Override
    public ResultadoSincronizacionRnt sincronizar() {
        return prestadores.sincronizar();
    }
}
