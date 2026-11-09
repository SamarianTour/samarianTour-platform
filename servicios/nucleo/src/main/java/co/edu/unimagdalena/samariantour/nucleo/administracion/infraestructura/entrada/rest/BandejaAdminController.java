package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto.AvisoBandejaResponse;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.AvisoAdministrado;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaAdministracionApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Los avisos al catálogo que el catálogo rechazó, y su reenvío (DN-44). Un rechazado no se reintenta solo: el
 * administrador lo reenvía después de corregir la causa. Los pide por {@code bandeja.api}.
 */
@RestController
@RequestMapping("/admin/bandeja")
public class BandejaAdminController {

    private final BandejaAdministracionApi bandeja;

    public BandejaAdminController(BandejaAdministracionApi bandeja) {
        this.bandeja = bandeja;
    }

    @GetMapping("/rechazados")
    public List<AvisoBandejaResponse> rechazados() {
        return bandeja.rechazados().stream().map(BandejaAdminController::aRespuesta).toList();
    }

    /** Vuelve a dejarlo pendiente: la bandeja lo envía en los siguientes 15 s. */
    @PostMapping("/{idAviso}/reenvio")
    public AvisoBandejaResponse reenviar(@PathVariable String idAviso) {
        return aRespuesta(bandeja.reenviar(idAviso));
    }

    private static AvisoBandejaResponse aRespuesta(AvisoAdministrado aviso) {
        return new AvisoBandejaResponse(aviso.idAviso(), aviso.destino(), aviso.tipo(), aviso.estado(),
                aviso.intentos(), aviso.ultimoError(), aviso.creadoEn());
    }
}
