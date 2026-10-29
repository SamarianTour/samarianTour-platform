package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.ResultadoSincronizacionRnt;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.SincronizarRntAdminUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.SincronizacionRntResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El administrador ejecuta la sincronización con el RNT cuando quiera, además de la semanal (RF-76, DN-36), con el rol
 * ADMINISTRADOR (DN-05). Responde cuando termina.
 */
@RestController
@RequestMapping("/admin/rnt")
public class RntAdminController {

    private final SincronizarRntAdminUseCase sincronizacion;

    public RntAdminController(SincronizarRntAdminUseCase sincronizacion) {
        this.sincronizacion = sincronizacion;
    }

    @PostMapping("/sincronizacion")
    public SincronizacionRntResponse sincronizar() {
        ResultadoSincronizacionRnt resultado = sincronizacion.sincronizar();
        return new SincronizacionRntResponse(resultado.revisados(), resultado.retirados(), resultado.restituidos(),
                resultado.sinRespuesta(), resultado.interrumpida());
    }
}
